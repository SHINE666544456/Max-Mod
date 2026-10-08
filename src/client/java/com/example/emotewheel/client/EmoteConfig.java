package com.example.emotewheel.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.example.emotewheel.emote.Emotes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

/** Saved to config/emote_wheel.json. */
public final class EmoteConfig {
    public static final int PAGES = 3;
    public static final int SLOTS = 8;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("emote_wheel.json");
    private static EmoteConfig instance;

    /** PAGES * SLOTS emote ids; null/empty = empty slot. Index = page * SLOTS + slot. */
    public String[] slots = new String[PAGES * SLOTS];
    public List<String> favorites = new ArrayList<>();
    public boolean cancelOnMove = true;
    public boolean thirdPerson = true;
    public boolean showOthers = true;
    /** Cat ears + tail on yourself: 0 = never, 1 = during cat emotes, 2 = always (only you see 'always'). */
    public int catMode = 1;
    /** Emote particles: 0 = off, 1 = light (default, signature moves only), 2 = full (every emote). */
    public int fxMode = 1;
    /** Ear style on yourself: 0 = off, 1 = cat, 2 = fox, 3 = bunny, 4 = bear. */
    public int earStyle = 1;
    /** Tail style on yourself: 0 = off, 1 = cat, 2 = fox, 3 = bunny pom. */
    public int tailStyle = 1;
    /** Little pet sitting on your head: 0 = off, 1 = kitten, 2 = fox kit, 3 = bunny. */
    public int pet = 0;
    /** How many clones Multi-Shadow Clone Jutsu spawns (owner only). No upper limit. */
    public int multiCloneCount = 8;
    /** Hex colours (RRGGBB) for cat extras. Tail also auto-mixes darker/lighter neighbours. */
    public String earOuter = "2C1A12";
    public String earInner = "E8A0B0";
    public String tailColor = "2C1A12";
    /** Most recently played emote ids, newest first. */
    public List<String> recent = new ArrayList<>();
    /** The last emote you played (for the Replay key). */
    public String lastEmote = null;
    public int page = 0;
    /** Last library page in the full emote menu. */
    public int listPage = 0;
    /** Last filter tab in the full emote menu (0 = all, 1 = favorites, 2+ = category). */
    public int filterIdx = 0;

    private static final String[] DEFAULTS = {
        // page 1: greetings & reactions
        "wave", "bow", "salute", "blow_kiss", "clap", "thumbs_up", "big_wave", "peace_sign",
        // page 2: dances
        "disco", "floss", "dab", "twist", "robot", "headbang", "ymca", "macarena",
        // page 3: fun & maid
        "nya", "maid_curtsy", "heart_hands", "backflip", "sit", "t_pose", "cat_pounce", "zombie",
    };

    public static EmoteConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static EmoteConfig load() {
        EmoteConfig c = null;
        if (Files.exists(PATH)) {
            try (Reader r = Files.newBufferedReader(PATH)) {
                c = GSON.fromJson(r, EmoteConfig.class);
            } catch (Exception e) {
                System.err.println("[emote_wheel] Could not read config, using defaults: " + e);
            }
        }
        boolean fresh = c == null || c.slots == null;
        if (c == null) c = new EmoteConfig();
        if (c.slots == null || c.slots.length != PAGES * SLOTS) {
            String[] fixed = new String[PAGES * SLOTS];
            if (c.slots != null) System.arraycopy(c.slots, 0, fixed, 0, Math.min(c.slots.length, fixed.length));
            c.slots = fixed;
        }
        if (fresh) System.arraycopy(DEFAULTS, 0, c.slots, 0, DEFAULTS.length);
        if (c.favorites == null) c.favorites = new ArrayList<>();
        // drop ids that no longer exist
        for (int i = 0; i < c.slots.length; i++) {
            if (c.slots[i] != null && Emotes.byId(c.slots[i]) == null) c.slots[i] = null;
        }
        c.favorites.removeIf(id -> Emotes.byId(id) == null);
        if (c.recent == null) c.recent = new ArrayList<>();
        c.recent.removeIf(id -> Emotes.byId(id) == null);
        if (c.lastEmote != null && Emotes.byId(c.lastEmote) == null) c.lastEmote = null;
        if (c.fxMode < 0 || c.fxMode > 2) c.fxMode = 1;
        if (c.earStyle < 0 || c.earStyle > 4) c.earStyle = 1;
        if (c.tailStyle < 0 || c.tailStyle > 3) c.tailStyle = 1;
        if (c.pet < 0 || c.pet > 3) c.pet = 0;
        if (c.multiCloneCount < 1) c.multiCloneCount = 8;
        if (c.earOuter == null || c.earOuter.isBlank()) c.earOuter = "2C1A12";
        if (c.earInner == null || c.earInner.isBlank()) c.earInner = "E8A0B0";
        if (c.tailColor == null || c.tailColor.isBlank()) c.tailColor = "2C1A12";
        c.page = Math.floorMod(c.page, PAGES);
        if (c.filterIdx < 0) c.filterIdx = 0;
        if (c.listPage < 0) c.listPage = 0;
        return c;
    }

    public void save() {
        try (Writer w = Files.newBufferedWriter(PATH)) {
            GSON.toJson(this, w);
        } catch (IOException e) {
            System.err.println("[emote_wheel] Could not save config: " + e);
        }
    }

    public String slotId(int page, int slot) { return slots[page * SLOTS + slot]; }

    public void setSlot(int page, int slot, String id) {
        slots[page * SLOTS + slot] = id;
        save();
    }

    public boolean isFavorite(String id) { return favorites.contains(id); }

    /** Remember an emote you just played (feeds the Recent tab and the Replay key). */
    public void markUsed(String id) {
        recent.remove(id);
        recent.add(0, id);
        while (recent.size() > 12) recent.remove(recent.size() - 1);
        lastEmote = id;
        save();
    }

    public void toggleFavorite(String id) {
        if (!favorites.remove(id)) favorites.add(id);
        save();
    }
}
