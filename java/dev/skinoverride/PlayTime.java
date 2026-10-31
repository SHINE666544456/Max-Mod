package dev.skinoverride;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Counts how long you play, per world / server, and saves it. Time only counts while the game is
 * actually running in a world (paused singleplayer does not count), like Minecraft's own stat.
 */
public final class PlayTime {

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Long>>() { }.getType();

    private static Map<String, Long> totals = new HashMap<>();
    private static boolean loaded;
    private static String key;
    private static long session;
    private static int sinceSave;

    private PlayTime() {
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("skinoverride").resolve("playtime.json");
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path path = file();
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                Map<String, Long> read = GSON.fromJson(reader, MAP_TYPE);
                if (read != null) {
                    totals = new HashMap<>(read);
                }
            } catch (Exception e) {
                SkinOverrideClient.LOGGER.warn("Could not read {}", path, e);
            }
        }
    }

    public static void save() {
        if (!loaded) {
            return;
        }
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(totals, MAP_TYPE, writer);
            }
        } catch (IOException e) {
            SkinOverrideClient.LOGGER.warn("Could not save {}", path, e);
        }
    }

    /** A stable id for the current world or server, used to file per-world data. */
    public static String worldKey(MinecraftClient mc) {
        return currentKey(mc);
    }

    private static String currentKey(MinecraftClient mc) {
        if (mc.isInSingleplayer() && mc.getServer() != null) {
            return "world:" + mc.getServer().getSaveProperties().getLevelName();
        }
        ServerInfo info = mc.getCurrentServerEntry();
        if (info != null) {
            return "server:" + info.address;
        }
        return "other";
    }

    public static void tick(MinecraftClient mc) {
        ensureLoaded();
        if (mc.world == null) {
            if (key != null) {
                save();
                key = null;
            }
            return;
        }
        String k = currentKey(mc);
        if (!k.equals(key)) {
            if (key != null) {
                save();
            }
            key = k;
            session = 0;
        }
        if (mc.isPaused()) {
            return;
        }
        session++;
        totals.merge(key, 1L, Long::sum);
        if (++sinceSave >= 1200) {
            sinceSave = 0;
            save();
        }
    }

    public static long worldTicks() {
        return key == null ? 0 : totals.getOrDefault(key, 0L);
    }

    public static long sessionTicks() {
        return session;
    }

    public static long allTicks() {
        long sum = 0;
        for (long v : totals.values()) {
            sum += v;
        }
        return sum;
    }

    /** "3m 05s", "12h 03m". */
    public static String format(long ticks) {
        long seconds = ticks / 20;
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) {
            return h + "h " + (m < 10 ? "0" : "") + m + "m";
        }
        return m + "m " + (s < 10 ? "0" : "") + s + "s";
    }
}
