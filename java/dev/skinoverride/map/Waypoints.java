package dev.skinoverride.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.skinoverride.SkinOverrideClient;
import dev.skinoverride.chat.ChatColors;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Named locations, saved per world and dimension. */
public final class Waypoints {

    public static final class Waypoint {
        public String name = "Waypoint";
        public int x;
        public int y;
        public int z;
        public int color = 0x35D0BA;
        public boolean death;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE = new TypeToken<Map<String, List<Waypoint>>>() { }.getType();

    private static Map<String, List<Waypoint>> all = new HashMap<>();
    private static boolean loaded;
    private static String current = "";

    private Waypoints() {
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("skinoverride").resolve("waypoints.json");
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path path = file();
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                Map<String, List<Waypoint>> read = GSON.fromJson(reader, TYPE);
                if (read != null) {
                    all = new HashMap<>(read);
                }
            } catch (Exception e) {
                SkinOverrideClient.LOGGER.warn("Could not read {}", path, e);
            }
        }
    }

    public static void switchTo(String dimensionKey) {
        ensureLoaded();
        current = dimensionKey;
    }

    public static List<Waypoint> list() {
        ensureLoaded();
        return all.computeIfAbsent(current, k -> new ArrayList<>());
    }

    public static Waypoint add(String name, int x, int y, int z) {
        Waypoint w = new Waypoint();
        w.name = name;
        w.x = x;
        w.y = y;
        w.z = z;
        w.color = ChatColors.PALETTE[(list().size() * 5 + 4) % ChatColors.PALETTE.length].rgb();
        list().add(w);
        save();
        return w;
    }

    /** Key of the world + dimension the waypoint list currently belongs to. */
    public static String currentKey() {
        return current;
    }

    /** Adds a waypoint to any world / dimension list (used for shared waypoints). */
    public static Waypoint addTo(String key, String name, int x, int y, int z, int color) {
        ensureLoaded();
        Waypoint w = new Waypoint();
        w.name = name;
        w.x = x;
        w.y = y;
        w.z = z;
        w.color = color;
        all.computeIfAbsent(key, k -> new ArrayList<>()).add(w);
        save();
        return w;
    }

    public static void remove(Waypoint w) {
        list().remove(w);
        save();
    }

    public static void save() {
        ensureLoaded();
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(all, TYPE, writer);
            }
        } catch (IOException e) {
            SkinOverrideClient.LOGGER.warn("Could not save {}", path, e);
        }
    }
}
