package dev.skinoverride;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Holds which skin file is assigned to which player UUID, loads the PNGs as
 * textures, and builds the replacement SkinTextures used by the mixins.
 * Everything here is client-side only: nobody else sees these changes.
 */
public final class SkinStore {

    public static final String AUTO = "auto";
    public static final String WIDE = "wide";
    public static final String SLIM = "slim";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Config config = new Config();
    private static Path rootDir;
    private static Path skinsDir;

    /** While true the mixins return null so the GUI can read a player's REAL skin. */
    private static volatile boolean bypass;

    /** Loaded textures by file name. Only touched on the render thread. */
    private static final Map<String, Loaded> loaded = new HashMap<>();
    private static final Set<String> failed = new HashSet<>();
    private static final Map<String, String> failedReason = new HashMap<>();

    private SkinStore() {
    }

    // ---------- config model ----------

    private static class Assignment {
        String file;
        String model = AUTO;
    }

    private static class Config {
        /** player UUID -> active assignment */
        Map<String, Assignment> assignments = new HashMap<>();
        /** player UUID -> the last skin file ever applied (kept after a reset) */
        Map<String, String> history = new HashMap<>();
        /** file name -> "slim"/"wide" hint (e.g. from a Mojang lookup) */
        Map<String, String> fileModels = new HashMap<>();
        /** player UUID last selected in the GUI */
        String lastPlayer;
    }

    private record Loaded(Identifier id, AssetInfo.TextureAssetInfo asset, boolean slimDetected) {
    }

    // ---------- setup ----------

    public static void init() {
        rootDir = FabricLoader.getInstance().getConfigDir().resolve("skinoverride");
        skinsDir = rootDir.resolve("skins");
        try {
            Files.createDirectories(skinsDir);
        } catch (IOException e) {
            SkinOverrideClient.LOGGER.warn("Could not create {}", skinsDir, e);
        }
        load();
    }

    public static Path skinsDir() {
        return skinsDir;
    }

    private static Path configFile() {
        return rootDir.resolve("assignments.json");
    }

    private static void load() {
        Path file = configFile();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Config read = GSON.fromJson(reader, Config.class);
            if (read != null) {
                if (read.assignments == null) {
                    read.assignments = new HashMap<>();
                }
                if (read.history == null) {
                    read.history = new HashMap<>();
                }
                if (read.fileModels == null) {
                    read.fileModels = new HashMap<>();
                }
                config = read;
            }
        } catch (Exception e) {
            SkinOverrideClient.LOGGER.warn("Could not read {}, starting fresh", file, e);
        }
    }

    private static void save() {
        try (Writer writer = Files.newBufferedWriter(configFile(), StandardCharsets.UTF_8)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            SkinOverrideClient.LOGGER.warn("Could not save {}", configFile(), e);
        }
    }

    // ---------- settings ----------

    public static UUID lastPlayer() {
        try {
            return config.lastPlayer == null ? null : UUID.fromString(config.lastPlayer);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static void setLastPlayer(UUID id) {
        String value = id == null ? null : id.toString();
        if (value != null && !value.equals(config.lastPlayer)) {
            config.lastPlayer = value;
            save();
        }
    }

    public static void setBypass(boolean value) {
        bypass = value;
    }

    // ---------- listing / importing ----------

    /** PNG file names in the skins folder, sorted. */
    public static List<String> listSkins() {
        List<String> names = new ArrayList<>();
        try (Stream<Path> files = Files.list(skinsDir)) {
            files.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.toLowerCase(Locale.ROOT).endsWith(".png"))
                    .forEach(names::add);
        } catch (IOException e) {
            SkinOverrideClient.LOGGER.warn("Could not list {}", skinsDir, e);
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    /** Copies dropped/selected PNG files into the skins folder. Returns the new file names. */
    public static List<String> importFiles(List<Path> sources) {
        List<String> added = new ArrayList<>();
        for (Path source : sources) {
            String name = source.getFileName().toString();
            if (!name.toLowerCase(Locale.ROOT).endsWith(".png") || !Files.isRegularFile(source)) {
                continue;
            }
            String safe = name.replaceAll("[^A-Za-z0-9._ \\-()]", "_");
            try {
                Files.copy(source, skinsDir.resolve(safe), StandardCopyOption.REPLACE_EXISTING);
                added.add(safe);
            } catch (IOException e) {
                SkinOverrideClient.LOGGER.warn("Could not import {}", source, e);
            }
        }
        return added;
    }

    /** Checks that a skin PNG can be loaded (render thread). Returns an error message or null. */
    public static String validate(String file) {
        return ensureLoadedWithError(file);
    }

    public static void deleteFile(String file) {
        try {
            Files.deleteIfExists(skinsDir.resolve(file));
        } catch (IOException e) {
            SkinOverrideClient.LOGGER.warn("Could not delete {}", file, e);
        }
    }

    // ---------- assignments ----------

    public static String assignedFile(UUID player) {
        Assignment a = config.assignments.get(player.toString());
        return a == null ? null : a.file;
    }

    /** The last skin applied to this player, even if it has since been reset. */
    public static String lastFile(UUID player) {
        return config.history.get(player.toString());
    }

    public static String modelMode(UUID player) {
        Assignment a = config.assignments.get(player.toString());
        return a == null ? AUTO : a.model;
    }

    /** Render thread only (uploads the texture). Returns an error message or null. */
    public static String assign(UUID player, String file) {
        String error = ensureLoadedWithError(file);
        if (error != null) {
            return error;
        }
        Assignment a = config.assignments.computeIfAbsent(player.toString(), k -> new Assignment());
        a.file = file;
        config.history.put(player.toString(), file);
        config.lastPlayer = player.toString();
        save();
        return null;
    }

    /** Re-applies the last skin used on this player. Returns an error message or null. */
    public static String restoreLast(UUID player) {
        String file = lastFile(player);
        if (file == null) {
            return "No previous skin saved for this player.";
        }
        return assign(player, file);
    }

    public static void clear(UUID player) {
        config.assignments.remove(player.toString());
        save();
    }

    public static void clearAll() {
        config.assignments.clear();
        save();
    }

    public static void cycleModel(UUID player) {
        Assignment a = config.assignments.get(player.toString());
        if (a == null) {
            return;
        }
        a.model = switch (a.model) {
            case AUTO -> WIDE;
            case WIDE -> SLIM;
            default -> AUTO;
        };
        save();
    }

    public static void setFileModelHint(String file, boolean slim) {
        config.fileModels.put(file, slim ? SLIM : WIDE);
        save();
    }

    // ---------- textures ----------

    /** Drops every loaded texture so edited PNGs are re-read next time they are used. */
    public static void reload() {
        MinecraftClient client = MinecraftClient.getInstance();
        for (Loaded l : loaded.values()) {
            client.getTextureManager().destroyTexture(l.id());
        }
        loaded.clear();
        failed.clear();
        failedReason.clear();
    }

    private static String ensureLoadedWithError(String file) {
        if (ensureLoaded(file) != null) {
            return null;
        }
        return failedReason.getOrDefault(file, "Could not load " + file);
    }

    private static Loaded ensureLoaded(String file) {
        Loaded existing = loaded.get(file);
        if (existing != null) {
            return existing;
        }
        if (failed.contains(file) || !RenderSystem.isOnRenderThread()) {
            return null;
        }
        Path path = skinsDir.resolve(file);
        try (InputStream in = Files.newInputStream(path)) {
            NativeImage source = NativeImage.read(in);
            NativeImage processed;
            boolean slim;
            try {
                int scale = SkinImages.scaleOf(source);
                slim = SkinImages.detectSlim(source, scale);
                processed = SkinImages.process(source);
            } finally {
                source.close();
            }
            Identifier id = Identifier.of(SkinOverrideClient.MOD_ID, "skins/" + textureKey(file));
            MinecraftClient.getInstance().getTextureManager()
                    .registerTexture(id, new NativeImageBackedTexture(id::toString, processed));
            Loaded result = new Loaded(id, new AssetInfo.TextureAssetInfo(id, id), slim);
            loaded.put(file, result);
            failedReason.remove(file);
            return result;
        } catch (Exception e) {
            failed.add(file);
            String reason = e.getMessage() == null ? e.toString() : e.getMessage();
            failedReason.put(file, file + ": " + reason);
            SkinOverrideClient.LOGGER.warn("Could not load skin {}: {}", path, reason);
            return null;
        }
    }

    private static String textureKey(String file) {
        String lower = file.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-';
            sb.append(ok ? c : '_');
        }
        sb.append('_').append(Integer.toHexString(file.hashCode() & 0x7FFFFFFF));
        return sb.toString();
    }

    private static SkinTextures build(Loaded skin, String file, String mode, SkinTextures vanilla) {
        PlayerSkinType model;
        if (SLIM.equals(mode)) {
            model = PlayerSkinType.SLIM;
        } else if (WIDE.equals(mode)) {
            model = PlayerSkinType.WIDE;
        } else {
            String hint = config.fileModels.get(file);
            boolean slim = hint != null ? SLIM.equals(hint) : skin.slimDetected();
            model = slim ? PlayerSkinType.SLIM : PlayerSkinType.WIDE;
        }
        return new SkinTextures(
                skin.asset(),
                vanilla == null ? null : vanilla.cape(),
                vanilla == null ? null : vanilla.elytra(),
                model,
                vanilla != null && vanilla.secure());
    }

    // ---------- used by the mixins and the GUI ----------

    /** Returns replacement textures for this player, or null to leave the vanilla skin alone. */
    public static SkinTextures override(UUID player, SkinTextures vanilla) {
        if (bypass || player == null || config.assignments.isEmpty()) {
            return null;
        }
        Assignment a = config.assignments.get(player.toString());
        if (a == null || a.file == null) {
            return null;
        }
        Loaded skin = ensureLoaded(a.file);
        if (skin == null) {
            return null;
        }
        return build(skin, a.file, a.model, vanilla);
    }

    /** Textures for previewing a file in the GUI (render thread), or null if it can't load. */
    public static SkinTextures preview(String file, SkinTextures vanilla) {
        Loaded skin = ensureLoaded(file);
        return skin == null ? null : build(skin, file, AUTO, vanilla);
    }
}
