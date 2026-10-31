package dev.skinoverride;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Settings profiles: save every setting under a name, load it back later, and optionally link a profile to a
 * server or world so it switches on automatically when you join. When you leave, your normal settings come back.
 * Files live in config/skinoverride/profiles.
 */
public final class Profiles {

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() { }.getType();

    private static Map<String, String> servers = new HashMap<>();
    private static boolean loaded;
    /** world key seen on the last tick, null while not in a world */
    private static String lastKey;
    private static String activeServerProfile;
    /** your normal settings, kept while a server profile is applied */
    private static String baseSnapshot;

    private Profiles() {
    }

    // ---------- files ----------

    private static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("skinoverride").resolve("profiles");
    }

    private static Path serversFile() {
        return dir().resolve("_servers.json");
    }

    private static Path fileOf(String name) {
        return dir().resolve(name + ".json");
    }

    public static boolean validName(String name) {
        return name != null && name.matches("[A-Za-z0-9 _-]{1,24}") && !name.startsWith("_");
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path path = serversFile();
        if (Files.isRegularFile(path)) {
            try {
                Map<String, String> read = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), MAP_TYPE);
                if (read != null) {
                    servers = new HashMap<>(read);
                }
            } catch (IOException | RuntimeException e) {
                SkinOverrideClient.LOGGER.warn("Could not read {}", path, e);
            }
        }
    }

    private static void saveServers() {
        try {
            Files.createDirectories(dir());
            Files.writeString(serversFile(), GSON.toJson(servers, MAP_TYPE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            SkinOverrideClient.LOGGER.warn("Could not save {}", serversFile(), e);
        }
    }

    // ---------- profiles ----------

    public static List<String> list() {
        List<String> names = new ArrayList<>();
        if (Files.isDirectory(dir())) {
            try (Stream<Path> files = Files.list(dir())) {
                files.forEach(p -> {
                    String file = p.getFileName().toString();
                    if (file.endsWith(".json") && !file.startsWith("_")) {
                        names.add(file.substring(0, file.length() - 5));
                    }
                });
            } catch (IOException e) {
                SkinOverrideClient.LOGGER.warn("Could not list profiles", e);
            }
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public static boolean exists(String name) {
        return validName(name) && Files.isRegularFile(fileOf(name));
    }

    /** Saves all current settings under this name. */
    public static boolean save(String name) {
        if (!validName(name)) {
            return false;
        }
        try {
            Files.createDirectories(dir());
            Files.writeString(fileOf(name), ModConfig.get().toJson(), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            SkinOverrideClient.LOGGER.warn("Could not save profile {}", name, e);
            return false;
        }
    }

    /** Replaces all current settings with the saved ones. */
    public static boolean load(String name) {
        if (!exists(name)) {
            return false;
        }
        try {
            ModConfig read = ModConfig.fromJson(Files.readString(fileOf(name), StandardCharsets.UTF_8));
            if (read == null) {
                return false;
            }
            ModConfig cfg = ModConfig.get();
            cfg.copyFrom(read);
            cfg.save();
            return true;
        } catch (IOException e) {
            SkinOverrideClient.LOGGER.warn("Could not load profile {}", name, e);
            return false;
        }
    }

    public static boolean delete(String name) {
        if (!exists(name)) {
            return false;
        }
        try {
            Files.delete(fileOf(name));
        } catch (IOException e) {
            return false;
        }
        ensureLoaded();
        servers.values().removeIf(v -> v.equalsIgnoreCase(name));
        saveServers();
        if (name.equals(activeServerProfile)) {
            activeServerProfile = null;
        }
        return true;
    }

    // ---------- per-server links ----------

    public static String serverProfile(String key) {
        ensureLoaded();
        return key == null ? null : servers.get(key);
    }

    /** Links a profile to a world / server key; null removes the link. */
    public static void setServerProfile(String key, String profile) {
        ensureLoaded();
        if (key == null) {
            return;
        }
        if (profile == null) {
            servers.remove(key);
        } else {
            servers.put(key, profile);
        }
        saveServers();
    }

    /** The key used for the world or server you are in now, or null at the main menu. */
    public static String currentKey(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) {
            return null;
        }
        return PlayTime.worldKey(mc);
    }

    // ---------- automatic switching ----------

    public static void tick(MinecraftClient mc) {
        ensureLoaded();
        String key = currentKey(mc);
        if (Objects.equals(key, lastKey)) {
            return;
        }
        String previous = lastKey;
        lastKey = key;

        // leaving a world that used a server profile: keep whatever you changed there
        if (previous != null && activeServerProfile != null) {
            save(activeServerProfile);
        }
        if (key == null) {
            restoreBase();
            activeServerProfile = null;
            return;
        }
        String mapped = servers.get(key);
        if (mapped != null && exists(mapped)) {
            if (baseSnapshot == null) {
                baseSnapshot = ModConfig.get().toJson();
            }
            if (load(mapped)) {
                activeServerProfile = mapped;
                mc.inGameHud.setOverlayMessage(Text.literal("Profile \"" + mapped + "\" loaded for this server"), false);
                return;
            }
        }
        restoreBase();
        activeServerProfile = null;
    }

    private static void restoreBase() {
        if (baseSnapshot == null) {
            return;
        }
        ModConfig base = ModConfig.fromJson(baseSnapshot);
        baseSnapshot = null;
        if (base != null) {
            ModConfig cfg = ModConfig.get();
            cfg.copyFrom(base);
            cfg.save();
        }
    }

    /** Called when the game closes, so a server profile never replaces your normal settings on disk. */
    public static void shutdown() {
        if (activeServerProfile != null) {
            save(activeServerProfile);
            activeServerProfile = null;
        }
        restoreBase();
    }

    /** Name of the profile applied because of the current server, or null. */
    public static String activeServerProfile() {
        return activeServerProfile;
    }

    public static String shortKey(String key) {
        if (key == null) {
            return "(not in a world)";
        }
        return key.toLowerCase(Locale.ROOT).startsWith("server:") ? key.substring(7) : key.substring(Math.min(6, key.length()));
    }
}
