package dev.skinoverride;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Capes and wings, and the data other players with the mod send about theirs.
 * <p>
 * A cape or wings entry is a texture. The built-in ones come with the mod and are shown to other players who
 * have the mod (when the server has it too). Your own PNG files (config/skinoverride/capes and /wings) are only
 * visible to you. "Wings" re-textures the elytra, so they show while an elytra is worn.
 */
public final class Cosmetics {

    public record Preset(String id, String name) {
    }

    public static final Preset[] CAPES = {
            new Preset("sakura", "Sakura"), new Preset("midnight", "Midnight"), new Preset("leaf", "Leaf"),
            new Preset("ember", "Ember"), new Preset("ocean", "Ocean"), new Preset("mono", "Snow")
    };
    public static final Preset[] WINGS = {
            new Preset("sakura", "Sakura"), new Preset("angel", "Angel"), new Preset("ember", "Ember"),
            new Preset("void", "Void"), new Preset("crystal", "Crystal")
    };

    /** What another player told us about their look. */
    public static final class Remote {
        public String cape = "";
        public String wings = "";
        public ItemStack[] stacks;
        public int selected = -1;
    }

    private static final Map<UUID, Remote> REMOTE = new HashMap<>();
    private static final Map<String, AssetInfo.TextureAssetInfo> ASSETS = new HashMap<>();
    private static final Map<String, AssetInfo.TextureAssetInfo> LOCAL = new HashMap<>();
    private static final java.util.Set<String> LOCAL_FAILED = new java.util.HashSet<>();
    private static List<String> capeFiles = new ArrayList<>();
    private static List<String> wingFiles = new ArrayList<>();
    private static long filesCheckedMs;

    private Cosmetics() {
    }

    // ---------- presets and local files ----------

    private static boolean isPreset(boolean cape, String id) {
        for (Preset p : cape ? CAPES : WINGS) {
            if (p.id().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static Path dirFor(boolean cape) {
        return FabricLoader.getInstance().getConfigDir().resolve("skinoverride").resolve(cape ? "capes" : "wings");
    }

    private static void refreshFiles() {
        long now = System.currentTimeMillis();
        if (now - filesCheckedMs < 3000L) {
            return;
        }
        filesCheckedMs = now;
        capeFiles = listPngs(dirFor(true));
        wingFiles = listPngs(dirFor(false));
    }

    private static List<String> listPngs(Path dir) {
        List<String> out = new ArrayList<>();
        try {
            Files.createDirectories(dir);
            try (Stream<Path> files = Files.list(dir)) {
                files.forEach(p -> {
                    String name = p.getFileName().toString();
                    if (name.toLowerCase(Locale.ROOT).endsWith(".png")) {
                        out.add(name);
                    }
                });
            }
        } catch (IOException e) {
            // folder not readable: no local files
        }
        Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    /** "" (none), the built-in ids, then "local:file.png" for each PNG you dropped in the folder. */
    public static List<String> optionIds(boolean cape) {
        refreshFiles();
        List<String> ids = new ArrayList<>();
        ids.add("");
        for (Preset p : cape ? CAPES : WINGS) {
            ids.add(p.id());
        }
        for (String f : cape ? capeFiles : wingFiles) {
            ids.add("local:" + f);
        }
        return ids;
    }

    public static String label(boolean cape, String id) {
        if (id == null || id.isEmpty()) {
            return "None";
        }
        if (id.startsWith("local:")) {
            String f = id.substring(6);
            return f.length() > 11 ? f.substring(0, 10) + "~" : f;
        }
        for (Preset p : cape ? CAPES : WINGS) {
            if (p.id().equals(id)) {
                return p.name();
            }
        }
        return "None";
    }

    public static String[] labels(boolean cape) {
        List<String> ids = optionIds(cape);
        String[] out = new String[ids.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = label(cape, ids.get(i));
        }
        return out;
    }

    public static int indexOf(boolean cape, String id) {
        int i = optionIds(cape).indexOf(id == null ? "" : id);
        return Math.max(i, 0);
    }

    public static String idAt(boolean cape, int index) {
        List<String> ids = optionIds(cape);
        return index >= 0 && index < ids.size() ? ids.get(index) : "";
    }

    public static Path capesDir() {
        return dirFor(true);
    }

    public static Path wingsDir() {
        return dirFor(false);
    }

    // ---------- textures ----------

    private static AssetInfo.TextureAssetInfo presetAsset(boolean cape, String id) {
        String name = (cape ? "cape_" : "wings_") + id;
        AssetInfo.TextureAssetInfo asset = ASSETS.get(name);
        if (asset == null) {
            asset = new AssetInfo.TextureAssetInfo(
                    Identifier.of(SkinOverrideClient.MOD_ID, "cosmetic/" + name),
                    Identifier.of(SkinOverrideClient.MOD_ID, "textures/cosmetic/" + name + ".png"));
            ASSETS.put(name, asset);
        }
        return asset;
    }

    private static AssetInfo.TextureAssetInfo localAsset(boolean cape, String file) {
        String key = (cape ? "c:" : "w:") + file;
        AssetInfo.TextureAssetInfo existing = LOCAL.get(key);
        if (existing != null) {
            return existing;
        }
        if (LOCAL_FAILED.contains(key) || !RenderSystem.isOnRenderThread()) {
            return null;
        }
        Path path = dirFor(cape).resolve(file);
        try (InputStream in = Files.newInputStream(path)) {
            NativeImage image = NativeImage.read(in);
            StringBuilder sb = new StringBuilder();
            for (char ch : file.toLowerCase(Locale.ROOT).toCharArray()) {
                sb.append((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_' || ch == '-' ? ch : '_');
            }
            Identifier id = Identifier.of(SkinOverrideClient.MOD_ID,
                    "cosmetic_local/" + (cape ? "c_" : "w_") + sb + "_" + Integer.toHexString(file.hashCode() & 0x7FFFFFFF));
            MinecraftClient.getInstance().getTextureManager()
                    .registerTexture(id, new NativeImageBackedTexture(id::toString, image));
            AssetInfo.TextureAssetInfo asset = new AssetInfo.TextureAssetInfo(id, id);
            LOCAL.put(key, asset);
            return asset;
        } catch (IOException | RuntimeException e) {
            LOCAL_FAILED.add(key);
            SkinOverrideClient.LOGGER.warn("Could not load {} {}: {}", cape ? "cape" : "wings", path, e.toString());
            return null;
        }
    }

    private static AssetInfo.TextureAssetInfo assetFor(boolean cape, String id, boolean allowLocal) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        if (id.startsWith("local:")) {
            return allowLocal ? localAsset(cape, id.substring(6)) : null;
        }
        return isPreset(cape, id) ? presetAsset(cape, id) : null;
    }

    /** Returns the skin with your chosen (or that player's shared) cape and wings, or null if nothing changes. */
    public static SkinTextures apply(UUID uuid, SkinTextures base) {
        if (base == null || uuid == null) {
            return null;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return null;
        }
        ModConfig cfg = ModConfig.get();
        String cape = null;
        String wings = null;
        boolean own = uuid.equals(mc.player.getUuid());
        if (own) {
            cape = cfg.capeId;
            wings = cfg.wingsId;
        } else if (cfg.showOthersCosmetics) {
            Remote r = REMOTE.get(uuid);
            if (r != null) {
                cape = r.cape;
                wings = r.wings;
            }
        }
        AssetInfo.TextureAssetInfo c = assetFor(true, cape, own);
        AssetInfo.TextureAssetInfo w = assetFor(false, wings, own);
        if (c == null && w == null) {
            return null;
        }
        return new SkinTextures(base.body(), c != null ? c : base.cape(), w != null ? w : base.elytra(),
                base.model(), base.secure());
    }

    // ---------- other players ----------

    public static Remote remote(UUID id) {
        return REMOTE.get(id);
    }

    public static void clearRemotes() {
        REMOTE.clear();
    }

    /** Text sent to the server: cape, wings and (if Weapon Master is on and shared) your hotbar weapons. */
    public static String localData(MinecraftClient mc) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.cosmeticsShare || mc.player == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (isPreset(true, cfg.capeId)) {
            sb.append("c=").append(cfg.capeId).append(';');
        }
        if (isPreset(false, cfg.wingsId)) {
            sb.append("w=").append(cfg.wingsId).append(';');
        }
        if (cfg.wmOn && cfg.wmShare) {
            sb.append("m=");
            boolean any = false;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                boolean weapon = WeaponMasterFeature.isWeapon(stack);
                any |= weapon;
                sb.append(i == 0 ? "" : ",").append(weapon ? Registries.ITEM.getId(stack.getItem()).toString() : "-");
            }
            sb.append(";s=").append(mc.player.getInventory().getSelectedSlot()).append(';');
            if (!any) {
                sb.append("x=1;");
            }
        }
        return sb.toString();
    }

    /** Stores what another player sent. Anything unknown or malformed is ignored. */
    public static void setRemote(UUID id, String data) {
        if (data == null || data.isEmpty() || data.length() > 400) {
            REMOTE.remove(id);
            return;
        }
        Remote r = new Remote();
        for (String part : data.split(";")) {
            int eq = part.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String k = part.substring(0, eq);
            String v = part.substring(eq + 1);
            switch (k) {
                case "c" -> r.cape = isPreset(true, v) ? v : "";
                case "w" -> r.wings = isPreset(false, v) ? v : "";
                case "s" -> {
                    try {
                        r.selected = Integer.parseInt(v);
                    } catch (NumberFormatException e) {
                        r.selected = -1;
                    }
                }
                case "m" -> {
                    String[] items = v.split(",", -1);
                    if (items.length == 9) {
                        ItemStack[] stacks = new ItemStack[9];
                        for (int i = 0; i < 9; i++) {
                            stacks[i] = ItemStack.EMPTY;
                            Identifier itemId = Identifier.tryParse(items[i]);
                            if (itemId != null && Registries.ITEM.containsId(itemId)) {
                                Item item = Registries.ITEM.get(itemId);
                                ItemStack stack = new ItemStack(item);
                                if (WeaponMasterFeature.isWeapon(stack)) {
                                    stacks[i] = stack;
                                }
                            }
                        }
                        r.stacks = stacks;
                    }
                }
                default -> {
                }
            }
        }
        REMOTE.put(id, r);
    }
}
