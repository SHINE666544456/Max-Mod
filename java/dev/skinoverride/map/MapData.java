package dev.skinoverride.map;

import dev.skinoverride.DayClock;
import dev.skinoverride.ModConfig;
import dev.skinoverride.PlayTime;
import dev.skinoverride.SkinOverrideClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The map's memory: the top-down color of every block column around you, remembered chunk by chunk
 * (and saved per world), drawn from loaded chunks only. Also owns the minimap texture.
 */
public final class MapData {

    /** Minimap texture size in blocks (one pixel per block). */
    public static final int TEX = 192;
    private static final int MAGIC = 0x49524953; // "IRIS"
    private static final int SCAN_RADIUS = 7;
    private static final int BACKGROUND = 0xFF0E1118;

    private static final Map<Long, int[]> CHUNKS = new HashMap<>();
    private static final Map<Long, Integer> LAST_SCAN = new HashMap<>();
    /** Cave mode: a slice of the cave at your height, never saved (it changes as you move up and down). */
    private static final Map<Long, int[]> CAVE = new HashMap<>();
    private static boolean caveActive;
    private static int caveY;

    private static String currentKey;
    private static boolean dirty;
    private static int version;
    private static int ticks;
    private static int saveTimer;
    private static boolean wasDead;

    private static NativeImageBackedTexture miniTexture;
    private static final Identifier MINI_ID = Identifier.of(SkinOverrideClient.MOD_ID, "minimap");
    private static int miniCenterX;
    private static int miniCenterZ;
    private static boolean miniReady;

    private MapData() {
    }

    // ---------- public queries ----------

    public static int version() {
        return version;
    }

    /** True while the minimap shows the cave slice instead of the surface. */
    public static boolean cave() {
        return caveActive;
    }

    public static boolean miniReady() {
        return miniReady;
    }

    public static Identifier miniId() {
        return MINI_ID;
    }

    public static int miniCenterX() {
        return miniCenterX;
    }

    public static int miniCenterZ() {
        return miniCenterZ;
    }

    private static long key(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    /** ARGB color remembered for a block column, or 0 if nothing is known there. */
    public static int colorAt(int bx, int bz) {
        int[] arr = CHUNKS.get(key(bx >> 4, bz >> 4));
        return arr == null ? 0 : arr[((bz & 15) << 4) | (bx & 15)];
    }

    /** The remembered colors of one chunk (256 entries, row = z, column = x) or null. */
    public static int[] chunk(int cx, int cz) {
        return CHUNKS.get(key(cx, cz));
    }

    public static int backgroundColor() {
        return BACKGROUND;
    }

    // ---------- tick ----------

    public static void tick(MinecraftClient mc) {
        ModConfig cfg = ModConfig.get();
        if (mc.world == null || mc.player == null) {
            if (currentKey != null) {
                saveNow(true);
                currentKey = null;
                CHUNKS.clear();
                LAST_SCAN.clear();
                miniReady = false;
                version++;
            }
            return;
        }
        if (!cfg.mapOn) {
            return;
        }
        String key = PlayTime.worldKey(mc) + "|" + mc.world.getRegistryKey().getValue();
        if (!key.equals(currentKey)) {
            saveNow(true);
            CHUNKS.clear();
            CAVE.clear();
            LAST_SCAN.clear();
            caveActive = false;
            currentKey = key;
            if (cfg.mapSave) {
                load(key);
            }
            Waypoints.switchTo(key);
            miniReady = false;
            version++;
        }
        ticks++;
        updateCave(mc, cfg);
        scan(mc, caveActive ? 4 : 2);
        if (ticks % 4 == 0) {
            refreshMini(mc);
        }
        if (++saveTimer >= 1200) {
            saveTimer = 0;
            saveNow(false);
        }
        trackDeath(mc, cfg);
    }

    private static void updateCave(MinecraftClient mc, ModConfig cfg) {
        boolean want = false;
        boolean nether = mc.world.getRegistryKey().getValue().getPath().equals("the_nether");
        if (!nether && cfg.mapCave != 0) {
            if (cfg.mapCave == 2) {
                want = true;
            } else {
                int top = mc.world.getTopY(Heightmap.Type.WORLD_SURFACE, mc.player.getBlockX(), mc.player.getBlockZ());
                want = top - mc.player.getBlockY() > 6;
            }
        }
        int py = mc.player.getBlockY();
        if (want != caveActive) {
            caveActive = want;
            caveY = py;
            CAVE.clear();
            LAST_SCAN.clear();
            version++;
        } else if (want && Math.abs(py - caveY) >= 3) {
            caveY = py;
            LAST_SCAN.clear(); // look again at the new height; old pixels stay until they are replaced
        }
    }

    private static void trackDeath(MinecraftClient mc, ModConfig cfg) {
        boolean dead = mc.player.getHealth() <= 0f;
        if (dead && !wasDead && cfg.mapDeathPoint) {
            Waypoints.list().removeIf(w -> w.death);
            Waypoints.Waypoint w = Waypoints.add("Death (day " + DayClock.day(0f) + ")",
                    mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ());
            w.death = true;
            w.color = 0xFF5555;
            Waypoints.save();
        }
        wasDead = dead;
    }

    // ---------- scanning ----------

    private static void scan(MinecraftClient mc, int budget) {
        World world = mc.world;
        int pcx = mc.player.getBlockX() >> 4;
        int pcz = mc.player.getBlockZ() >> 4;
        int yCap = mc.player.getBlockY() + 3;
        boolean nether = world.getRegistryKey().getValue().getPath().equals("the_nether");
        boolean cave = caveActive;
        int rescan = cave ? 30 : 100;

        for (int n = 0; n < budget; n++) {
            long bestKey = 0;
            int bestCx = 0;
            int bestCz = 0;
            long bestScore = Long.MAX_VALUE;
            for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
                for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                    int cx = pcx + dx;
                    int cz = pcz + dz;
                    if (!world.isChunkLoaded(cx, cz)) {
                        continue;
                    }
                    long k = key(cx, cz);
                    Integer last = LAST_SCAN.get(k);
                    long score;
                    if (last == null) {
                        score = -1_000_000L + (long) dx * dx + (long) dz * dz;
                    } else if (ticks - last < rescan) {
                        continue;
                    } else {
                        score = -(ticks - last) + (long) (dx * dx + dz * dz) * 2;
                    }
                    if (score < bestScore) {
                        bestScore = score;
                        bestKey = k;
                        bestCx = cx;
                        bestCz = cz;
                    }
                }
            }
            if (bestScore == Long.MAX_VALUE) {
                return;
            }
            scanChunk(world, bestCx, bestCz, yCap, nether, cave);
            LAST_SCAN.put(bestKey, ticks);
        }
    }

    private static void scanChunk(World world, int cx, int cz, int yCap, boolean nether, boolean cave) {
        int[] colors = new int[256];
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int bottom = world.getBottomY();
        boolean changed = false;
        Map<Long, int[]> target = cave ? CAVE : CHUNKS;
        int[] old = target.get(key(cx, cz));
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int x = cx * 16 + lx;
                int z = cz * 16 + lz;
                int c = cave ? caveColumn(world, pos, x, z, bottom, caveY + 1)
                        : columnColor(world, pos, x, z, bottom, yCap, nether);
                colors[(lz << 4) | lx] = c;
                if (old == null || old[(lz << 4) | lx] != c) {
                    changed = true;
                }
            }
        }
        if (changed) {
            target.put(key(cx, cz), colors);
            if (!cave) {
                dirty = true;
            }
            version++;
        }
    }

    private static int scale(int rgb, float f) {
        int r = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * f));
        int g = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * f));
        int b = Math.min(255, Math.round((rgb & 0xFF) * f));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int columnColor(World world, BlockPos.Mutable pos, int x, int z, int bottom, int yCap, boolean nether) {
        int top = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
        int y = top - 1;
        if (nether) {
            y = Math.min(y, yCap);
        }
        MapColor color = MapColor.CLEAR;
        for (; y >= bottom; y--) {
            pos.set(x, y, z);
            BlockState state = world.getBlockState(pos);
            color = state.getMapColor(world, pos);
            if (color != MapColor.CLEAR) {
                break;
            }
        }
        if (color == MapColor.CLEAR) {
            return 0;
        }
        int rgb = color.color;
        if (color == MapColor.WATER_BLUE) {
            int depth = 0;
            for (int yy = y - 1; yy >= bottom && depth < 24; yy--) {
                pos.set(x, yy, z);
                MapColor below = world.getBlockState(pos).getMapColor(world, pos);
                if (below != MapColor.WATER_BLUE && below != MapColor.CLEAR) {
                    rgb = mixRgb(color.color, below.color, 0.35f);
                    break;
                }
                depth++;
            }
            return scale(rgb, 1.0f - Math.min(depth, 18) * 0.03f);
        }
        float factor = 1.0f;
        if (world.isChunkLoaded(x >> 4, (z - 1) >> 4)) {
            int north = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z - 1);
            int diff = top - north;
            factor = diff >= 3 ? 1.18f : diff > 0 ? 1.08f : diff == 0 ? 1.0f : diff > -3 ? 0.9f : 0.78f;
        }
        return scale(rgb, factor);
    }

    private static boolean isOpen(World world, BlockPos pos, BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty() || state.getCollisionShape(world, pos).isEmpty();
    }

    /**
     * Cave view of one column: solid rock at your head height is drawn dark, open space shows the floor below it
     * (darker the deeper it is), and water or lava shows its own color.
     */
    private static int caveColumn(World world, BlockPos.Mutable pos, int x, int z, int bottom, int slice) {
        pos.set(x, slice, z);
        BlockState head = world.getBlockState(pos);
        if (!isOpen(world, pos, head)) {
            MapColor rock = head.getMapColor(world, pos);
            return rock == MapColor.CLEAR ? 0xFF1A1D26 : scale(rock.color, 0.30f);
        }
        for (int y = slice - 1; y >= bottom && y > slice - 48; y--) {
            pos.set(x, y, z);
            BlockState state = world.getBlockState(pos);
            if (!state.getFluidState().isEmpty()) {
                MapColor fluid = state.getMapColor(world, pos);
                if (fluid != MapColor.CLEAR) {
                    return scale(fluid.color, 0.85f);
                }
                continue;
            }
            if (isOpen(world, pos, state)) {
                continue;
            }
            MapColor c = state.getMapColor(world, pos);
            if (c == MapColor.CLEAR) {
                continue;
            }
            float f = Math.max(0.45f, 1.0f - (slice - 1 - y) * 0.06f);
            return scale(c.color, f);
        }
        return 0xFF0A0C12;
    }

    private static int mixRgb(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    // ---------- minimap texture ----------

    private static void refreshMini(MinecraftClient mc) {
        if (miniTexture == null) {
            miniTexture = new NativeImageBackedTexture(() -> "iris minimap", new NativeImage(TEX, TEX, true));
            mc.getTextureManager().registerTexture(MINI_ID, miniTexture);
        }
        NativeImage image = miniTexture.getImage();
        if (image == null) {
            return;
        }
        int px = mc.player.getBlockX();
        int pz = mc.player.getBlockZ();
        int half = TEX / 2;
        long lastKey = Long.MIN_VALUE;
        int[] arr = null;
        Map<Long, int[]> src = caveActive ? CAVE : CHUNKS;
        for (int j = 0; j < TEX; j++) {
            int bz = pz - half + j;
            for (int i = 0; i < TEX; i++) {
                int bx = px - half + i;
                long k = key(bx >> 4, bz >> 4);
                if (k != lastKey) {
                    arr = src.get(k);
                    lastKey = k;
                }
                int c = arr == null ? 0 : arr[((bz & 15) << 4) | (bx & 15)];
                image.setColorArgb(i, j, c == 0 ? BACKGROUND : c);
            }
        }
        miniTexture.upload();
        miniCenterX = px;
        miniCenterZ = pz;
        miniReady = true;
    }

    // ---------- saving / loading ----------

    private static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("skinoverride").resolve("maps");
    }

    private static Path fileFor(String key) {
        String safe = key.replaceAll("[^A-Za-z0-9._-]", "_");
        if (safe.length() > 80) {
            safe = safe.substring(0, 80);
        }
        return dir().resolve(safe + "_" + Integer.toHexString(key.hashCode()) + ".bin");
    }

    /** Saves in the background (or right now when shutting down / changing world). */
    public static void saveNow(boolean wait) {
        if (!dirty || currentKey == null || !ModConfig.get().mapSave) {
            return;
        }
        Map<Long, int[]> snapshot = new HashMap<>(CHUNKS);
        Path target = fileFor(currentKey);
        dirty = false;
        Runnable job = () -> write(snapshot, target);
        if (wait) {
            job.run();
        } else {
            Thread t = new Thread(job, "iris-map-save");
            t.setDaemon(true);
            t.start();
        }
    }

    private static void write(Map<Long, int[]> snapshot, Path target) {
        try {
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            try (DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(tmp))))) {
                out.writeInt(MAGIC);
                out.writeInt(snapshot.size());
                for (Map.Entry<Long, int[]> e : snapshot.entrySet()) {
                    out.writeLong(e.getKey());
                    for (int c : e.getValue()) {
                        out.writeInt(c);
                    }
                }
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            SkinOverrideClient.LOGGER.warn("Could not save map {}", target, e);
        }
    }

    private static void load(String key) {
        Path path = fileFor(key);
        if (!Files.isRegularFile(path)) {
            return;
        }
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new GZIPInputStream(Files.newInputStream(path))))) {
            if (in.readInt() != MAGIC) {
                return;
            }
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                long k = in.readLong();
                int[] arr = new int[256];
                for (int j = 0; j < 256; j++) {
                    arr[j] = in.readInt();
                }
                CHUNKS.put(k, arr);
            }
        } catch (IOException e) {
            SkinOverrideClient.LOGGER.warn("Could not read map {}", path, e);
        }
    }

    /** Forgets everything remembered for the current world (used by the "Clear map" button). */
    public static void clearCurrent() {
        CHUNKS.clear();
        CAVE.clear();
        LAST_SCAN.clear();
        dirty = true;
        version++;
        saveNow(false);
    }
}
