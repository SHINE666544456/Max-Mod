package dev.skinoverride;

import dev.skinoverride.chat.ChatColors;
import dev.skinoverride.chat.Emotes;
import dev.skinoverride.hud.HudManager;
import dev.skinoverride.hud.HudWidget;
import dev.skinoverride.map.MapData;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

/** Tabbed settings screen: toggles, sliders, color pickers and option cyclers for every feature. */
public class SettingsScreen extends ThemedScreen {

    private sealed interface Row permits Header, Note, Toggle, Slider, Cycle, ColorRow, Action, EmoteGrid, ArmorGrid {
    }

    private record Header(String text) implements Row {
    }

    private record Note(String text) implements Row {
    }

    private record Toggle(String label, String desc, BooleanSupplier get, Consumer<Boolean> set) implements Row {
    }

    private record Slider(String label, String desc, int min, int max, int step, IntFunction<String> format,
                          IntSupplier get, IntConsumer set) implements Row {
    }

    private record Cycle(String label, String desc, String[] options, IntSupplier get, IntConsumer set) implements Row {
    }

    /** plain = only real colors (no auto / rainbow / cycle), used by the GUI theme. */
    private record ColorRow(String label, String desc, IntSupplier get, IntConsumer set, boolean plain) implements Row {
        ColorRow(String label, String desc, IntSupplier get, IntConsumer set) {
            this(label, desc, get, set, false);
        }
    }

    private record Action(String label, String desc, String button, Runnable run) implements Row {
    }

    private record EmoteGrid() implements Row {
    }

    private record ArmorGrid() implements Row {
    }

    private static final String[] TABS = {"HUD", "Info", "Players", "Chat", "Visuals", "Combat", "Map", "Tools", "Theme", "Profiles", "Cosmetics"};
    private static final int TAB_STEP = 16;
    /** which rows() branch each tab uses */
    private static final int[] TAB_CASE = {0, 1, 2, 4, 5, 6, 8, 9, 10, 11, 12};
    private static final int TAB_W = 78;
    private static final int SLIDER_W = 100;

    private final Screen parent;
    private final ModConfig cfg = ModConfig.get();

    private int tab;
    private double scroll;
    private Slider dragging;
    private int dragX;
    private ColorRow picker;
    private int pickerX;
    private int pickerY;

    // panel geometry
    private int px, py, pw, ph, contentX, contentW, contentTop, contentBottom;

    public SettingsScreen(Screen parent) {
        super(Text.literal("Settings"));
        this.parent = parent;
    }

    // ---------- rows ----------

    private static final String[] PRESET_NAMES = {
            "Custom", "Teal", "Violet", "Ocean", "Rose", "Amber", "Forest", "Mono", "Cherry Blossom", "Pink Night"
    };
    // accent, background for presets 1..7
    private static final int[][] PRESETS = {
            {0x35D0BA, 0x111420}, {0xA78BFA, 0x15121F}, {0x4DA3FF, 0x0F1724}, {0xFF6B9D, 0x1E1218},
            {0xFFB347, 0x1C1710}, {0x5BD66F, 0x111A14}, {0xD0D4DC, 0x15161A},
            {0xFFB7D5, 0x1D1218}, {0xFF4FA3, 0x111420}
    };

    private void applyPreset(int index) {
        cfg.themePreset = index;
        if (index > 0 && index <= PRESETS.length) {
            cfg.themeAccent = PRESETS[index - 1][0];
            cfg.themeBg = PRESETS[index - 1][1];
        }
        Ui.applyTheme(cfg);
    }

    private List<Row> rows() {
        List<Row> r = new ArrayList<>();
        switch (TAB_CASE[tab]) {
            case 0 -> {
                r.add(new Header("Armor & held items"));
                r.add(new Toggle("Show armor HUD", "Your armor and held items. Drag it anywhere.",
                        () -> cfg.armorHud, v -> cfg.armorHud = v));
                r.add(new Toggle("Show held item", "Main-hand item next to your armor.",
                        () -> cfg.armorHeld, v -> cfg.armorHeld = v));
                r.add(new Toggle("Show off-hand", "Off-hand item too.",
                        () -> cfg.armorOffhand, v -> cfg.armorOffhand = v));
                r.add(new Cycle("Direction", "Row or column (for next to the hotbar).",
                        new String[]{"Horizontal", "Vertical"}, () -> cfg.armorLayout, v -> cfg.armorLayout = v));
                r.add(new Cycle("Durability text", "Number next to each damageable item.",
                        new String[]{"Off", "Remaining", "Percent"},
                        () -> cfg.armorDurability, v -> cfg.armorDurability = v));
                r.add(new Toggle("Low durability warning", "The slot flashes red when low.",
                        () -> cfg.armorLowWarning, v -> cfg.armorLowWarning = v));
                r.add(new Slider("Warn below", "Percent of durability left.", 5, 50, 5,
                        v -> v + "%", () -> cfg.armorLowPercent, v -> cfg.armorLowPercent = v));
                r.add(new Header("Effect timers"));
                r.add(new Toggle("Custom effect timers", "Replaces the vanilla effect icons.",
                        () -> cfg.effectsHud, v -> cfg.effectsHud = v));
                r.add(new Note("The bar drains from green to yellow, orange and red as time runs out."));
                r.add(new Toggle("Flash when low", "Blinks red as the effect runs out.",
                        () -> cfg.effectsFlash, v -> cfg.effectsFlash = v));
                r.add(new Slider("Low at", "Seconds left before it counts as low.", 3, 30, 1,
                        v -> v + "s", () -> cfg.effectsLowSeconds, v -> cfg.effectsLowSeconds = v));
                r.add(new Header("Keystrokes & CPS"));
                r.add(new Toggle("Show keystrokes", "WASD that lights up when you press it.",
                        () -> cfg.keystrokesHud, v -> cfg.keystrokesHud = v));
                r.add(new Toggle("Space bar", "Jump key.",
                        () -> cfg.keystrokesSpace, v -> cfg.keystrokesSpace = v));
                r.add(new Toggle("Mouse buttons", "LMB and RMB.",
                        () -> cfg.keystrokesMouse, v -> cfg.keystrokesMouse = v));
                r.add(new Toggle("Clicks per second", "CPS under each mouse button.",
                        () -> cfg.keystrokesCps, v -> cfg.keystrokesCps = v));
                r.add(new Header("Join / leave pop-ups"));
                r.add(new Toggle("Pop-ups", "A small notice when a player joins or leaves.",
                        () -> cfg.notifyPopups, v -> cfg.notifyPopups = v));
                r.add(new Toggle("Joins", "When someone comes online.",
                        () -> cfg.notifyJoin, v -> cfg.notifyJoin = v));
                r.add(new Toggle("Leaves", "When someone goes offline.",
                        () -> cfg.notifyLeave, v -> cfg.notifyLeave = v));
                r.add(new Toggle("Sound", "A soft click with each pop-up.",
                        () -> cfg.notifySound, v -> cfg.notifySound = v));
                r.add(new Toggle("Only my listed players", "Only players with custom colors (Chat > Per-player).",
                        () -> cfg.notifyOnlyListed, v -> cfg.notifyOnlyListed = v));
                r.add(new Note("Every HUD element can be moved and resized: Edit HUD layout (below)."));
                r.add(new Header("Shown HUD elements"));
                for (HudWidget hw : HudManager.WIDGETS) {
                    r.add(new Toggle(hw.label(), "Off = hidden, but its settings are kept.",
                            () -> !HudManager.isHidden(hw), v -> HudManager.setHidden(hw, !v)));
                }
                r.add(new Toggle("Villager trades panel", "Point at a villager to see its enchanted book trades.",
                        () -> cfg.villagerHudOn, v -> cfg.villagerHudOn = v));
                r.add(new Note("A key (Controls: \"Hide / show all IRIS HUD panels\") hides them all at once."));
            }
            case 1 -> {
                r.add(new Header("Coordinates & FPS"));
                r.add(new Toggle("Show info panel", "A small movable panel.",
                        () -> cfg.infoHud, v -> cfg.infoHud = v));
                r.add(new Toggle("Coordinates", "Your X / Y / Z.",
                        () -> cfg.infoCoords, v -> cfg.infoCoords = v));
                r.add(new Toggle("Facing direction", "Compass direction you look.",
                        () -> cfg.infoDirection, v -> cfg.infoDirection = v));
                r.add(new Toggle("FPS", "Frames per second.",
                        () -> cfg.infoFps, v -> cfg.infoFps = v));
                r.add(new Toggle("Ping", "Latency to the server.",
                        () -> cfg.infoPing, v -> cfg.infoPing = v));
                r.add(new Toggle("Light level", "Block and sky light where you stand.",
                        () -> cfg.infoLight, v -> cfg.infoLight = v));
                r.add(new Header("Day counter & clock"));
                r.add(new Toggle("Day & time on HUD", "Permanent day counter with a ticking clock.",
                        () -> cfg.clockHud, v -> cfg.clockHud = v));
                r.add(new Toggle("Show seconds", "Exact ticking time of day.",
                        () -> cfg.clockSeconds, v -> cfg.clockSeconds = v));
                r.add(new Toggle("24-hour clock", "Off = AM / PM.",
                        () -> cfg.clock24h, v -> cfg.clock24h = v));
                r.add(new Toggle("Playtime line", "Total time played in this world / server.",
                        () -> cfg.clockPlaytime, v -> cfg.clockPlaytime = v));
                r.add(new Toggle("Info on the pause menu", "Day, exact time, sunset, playtime.",
                        () -> cfg.pauseInfo, v -> cfg.pauseInfo = v));
                r.add(new Header("Subtitles"));
                r.add(new Toggle("Movable subtitles", "Replaces the vanilla sound captions.",
                        () -> cfg.subtitlesHud, v -> cfg.subtitlesHud = v));
                r.add(new Toggle("Show subtitles", "Minecraft's own subtitles option.",
                        () -> this.client.options.getShowSubtitles().getValue(),
                        v -> this.client.options.getShowSubtitles().setValue(v)));
            }
            case 2 -> {
                r.add(new Header("Crosshair inspect"));
                r.add(new Toggle("Enable inspect overlay", "Shows gear of the player you look at.",
                        () -> cfg.inspectHud, v -> cfg.inspectHud = v));
                r.add(new Toggle("Show enchantments", "Lists enchants for each held/worn item.",
                        () -> cfg.inspectEnchants, v -> cfg.inspectEnchants = v));
                r.add(new Toggle("Show health", "Their current health.",
                        () -> cfg.inspectHealth, v -> cfg.inspectHealth = v));
                r.add(new Slider("Range", "How far away to inspect.", 8, 64, 4,
                        v -> v + " blocks", () -> cfg.inspectRange, v -> cfg.inspectRange = v));
                r.add(new Note("Only players you can see are shown, and only held/worn items:"));
                r.add(new Note("servers never send other players' inventories to your client."));
                r.add(new Header("AFK detection"));
                r.add(new Toggle("Enable AFK detection", "Watches nearby players for movement.",
                        () -> cfg.afkTracking, v -> cfg.afkTracking = v));
                r.add(new Slider("Mark AFK after", "Time without moving or turning.", 15, 600, 15,
                        Ui::formatShort, () -> cfg.afkSeconds, v -> cfg.afkSeconds = v));
                r.add(new Toggle("AFK tag over heads", "A small AFK label above their head.",
                        () -> cfg.afkTags, v -> cfg.afkTags = v));
                r.add(new Toggle("Show AFK list on HUD", "Small movable list of AFK players.",
                        () -> cfg.afkHudList, v -> cfg.afkHudList = v));
                r.add(new Note("AFK also shows in the skin screen and the inspect overlay."));
                r.add(new Note("Only players loaded near you can be watched. Being pushed"));
                r.add(new Note("by water or pistons counts as moving."));
            }
            case 4 -> {
                r.add(new Header("Chat colors"));
                r.add(new Toggle("Custom chat colors", "Master switch for the colors below.",
                        () -> cfg.chatCustom, v -> cfg.chatCustom = v));
                r.add(new ColorRow("Everyone's name color", "Used unless a player has their own.",
                        () -> cfg.chatDefaultNameColor, v -> cfg.chatDefaultNameColor = v));
                r.add(new ColorRow("Everyone's message color", "Used unless a player has their own.",
                        () -> cfg.chatDefaultMsgColor, v -> cfg.chatDefaultMsgColor = v));
                r.add(new Action("Per-player colors", "Set a name + message color for specific players.", "Open",
                        () -> this.client.setScreen(new ChatPlayersScreen(this))));
                r.add(new ColorRow("Join messages", "\"X joined the game\".",
                        () -> cfg.chatJoinColor, v -> cfg.chatJoinColor = v));
                r.add(new ColorRow("Leave messages", "\"X left the game\".",
                        () -> cfg.chatLeaveColor, v -> cfg.chatLeaveColor = v));
                r.add(new ColorRow("Advancements", "Advancement and challenge messages.",
                        () -> cfg.chatAdvancementColor, v -> cfg.chatAdvancementColor = v));
                r.add(new ColorRow("Death messages", "How players died.",
                        () -> cfg.chatDeathColor, v -> cfg.chatDeathColor = v));
                r.add(new Note("Rainbow colors every letter. Color cycle gives each new message"));
                r.add(new Note("a different color. Colors are fixed once a message arrives."));
                r.add(new Header("Chat heads"));
                r.add(new Toggle("Player heads in chat", "A small face in front of each player's message.",
                        () -> cfg.chatHeads, v -> cfg.chatHeads = v));
                r.add(new Note("Uses the game's built-in player heads, so they show the real skin."));
                r.add(new Header("Emotes"));
                r.add(new Toggle("Emotes", "Show :smile: :heart: ... as pixel emotes.",
                        () -> cfg.chatEmotes, v -> cfg.chatEmotes = v));
                r.add(new Toggle("Text faces", "Also turn :) :D <3 into emotes.",
                        () -> cfg.chatEmoteFaces, v -> cfg.chatEmoteFaces = v));
                r.add(new EmoteGrid());
                r.add(new Note("Armor icons: letter + piece, like :Dchest: or :LLeg: (L C I G D N)."));
                r.add(new ArmorGrid());
                r.add(new Note("Only you see emotes. Animated GIFs can't be shown in chat text."));
                r.add(new Header("Speech bubbles"));
                r.add(new Toggle("Bubbles above heads", "Chat messages float above nearby players.",
                        () -> cfg.chatBubbles, v -> cfg.chatBubbles = v));
                r.add(new Slider("Bubble time", "How long a bubble stays.", 3, 15, 1,
                        v -> v + "s", () -> cfg.chatBubbleSeconds, v -> cfg.chatBubbleSeconds = v));
                r.add(new Slider("Bubble range", "How close the player must be.", 8, 64, 4,
                        v -> v + " blocks", () -> cfg.chatBubbleRange, v -> cfg.chatBubbleRange = v));
                r.add(new Header("Text to speech"));
                r.add(new Toggle("Read chat aloud", "Uses Minecraft's built-in narrator.",
                        () -> cfg.chatTts, Tts::set));
                r.add(new Note("The voice and speed come from your operating system."));
                r.add(new Header("Timestamps"));
                r.add(new Toggle("Timestamps", "A small grey [12:34] in front of every chat line.",
                        () -> cfg.chatTimestamps, v -> cfg.chatTimestamps = v));
                r.add(new Toggle("24-hour time", "Off = 12-hour with AM / PM.",
                        () -> cfg.chatTime24h, v -> cfg.chatTime24h = v));
                r.add(new Toggle("Show seconds", "[12:34:56] instead of [12:34].",
                        () -> cfg.chatTimeSeconds, v -> cfg.chatTimeSeconds = v));
                r.add(new Header("Name mentions"));
                r.add(new Toggle("Notice my name", "When someone else says your name in chat.",
                        () -> cfg.chatMention, v -> cfg.chatMention = v));
                r.add(new Toggle("Play a sound", "A short sound for each mention.",
                        () -> cfg.chatMentionSound, v -> cfg.chatMentionSound = v));
                r.add(new Cycle("Sound", "Which sound to play.", new String[]{"XP orb", "Level up", "Chime"},
                        () -> cfg.chatMentionSoundChoice, v -> {
                            cfg.chatMentionSoundChoice = v;
                            dev.skinoverride.chat.ChatExtras.playSound(this.client, cfg);
                        }));
                r.add(new Toggle("Highlight the message", "Colors the whole line so you spot it.",
                        () -> cfg.chatMentionHighlight, v -> cfg.chatMentionHighlight = v));
                r.add(new ColorRow("Highlight color", "Color of a message that mentions you.",
                        () -> cfg.chatMentionColor, v -> cfg.chatMentionColor = v, true));
                r.add(new Note("More words (a nickname...): chatMentionWords in config/skinoverride/settings.json,"));
                r.add(new Note("separated by commas."));
                r.add(new Header("Search"));
                r.add(new Action("Search chat", "Find anything said this session. Click a line to copy it.", "Open",
                        () -> this.client.setScreen(new ChatSearchScreen(this))));
            }
            case 5 -> {
                r.add(new Header("World"));
                r.add(new Toggle("Reveal invisible players", "Shows invisible players and mobs normally.",
                        () -> cfg.revealInvisible, v -> cfg.revealInvisible = v));
                r.add(new Note("Only changes what YOU see. Invisibility still works for everyone else."));
                r.add(new Toggle("Fullbright", "See clearly in the dark. Key: G.",
                        () -> cfg.fullbright, v -> cfg.fullbright = v));
                r.add(new Header("Zoom"));
                r.add(new Slider("Default zoom", "Hold C to zoom; scroll to change it.", 2, 10, 1,
                        v -> v + "x", () -> cfg.zoomDefault, v -> cfg.zoomDefault = v));
                r.add(new Toggle("Cinematic camera", "Smooths the mouse while zoomed. Off = direct aim.",
                        () -> cfg.zoomCinematic, v -> cfg.zoomCinematic = v));
                r.add(new Note("Zoom is instant: no animation, no pulling effect."));
                r.add(new Header("Freecam"));
                r.add(new Action("Toggle freecam", "Fly the camera around while your player stays put. Key: F8.", "Toggle",
                        () -> {
                            this.client.setScreen(null);
                            Freecam.toggle(this.client);
                        }));
                r.add(new Slider("Flight speed", "Blocks per second (scroll while flying; sprint = x3).", 1, 100, 1,
                        v -> v + " b/s", () -> cfg.freecamSpeed, v -> cfg.freecamSpeed = v));
                r.add(new Toggle("Show your player", "Draws your character where you left it.",
                        () -> cfg.freecamShowBody, v -> cfg.freecamShowBody = v));
                r.add(new Toggle("Block actions", "No mining, placing or picking while flying.",
                        () -> cfg.freecamBlockActions, v -> cfg.freecamBlockActions = v));
                r.add(new Toggle("Exit when hurt", "Leave freecam if you take damage.",
                        () -> cfg.freecamDamageOff, v -> cfg.freecamDamageOff = v));
                r.add(new Toggle("Indicator", "Small FREECAM label with the speed.",
                        () -> cfg.freecamIndicator, v -> cfg.freecamIndicator = v));
                r.add(new Header("Light level"));
                r.add(new Toggle("Light overlay", "Colored squares on the floor. Key: F7.",
                        () -> cfg.lightOverlay, v -> cfg.lightOverlay = v));
                r.add(new Note("Red = 0 (mobs can spawn), yellow = 1-7, green = 8 and up."));
                r.add(new Slider("Overlay radius", "How far around you.", 3, 10, 1,
                        v -> v + " blocks", () -> cfg.lightRadius, v -> cfg.lightRadius = v));
                r.add(new Toggle("Show numbers", "The light number on close squares.",
                        () -> cfg.lightNumbers, v -> cfg.lightNumbers = v));
                r.add(new Header("Nametags"));
                r.add(new ColorRow("Nametag color", "For everyone (per-player colors override it).",
                        () -> cfg.nametagDefaultColor, v -> cfg.nametagDefaultColor = v));
                r.add(new Action("Per-player nametags", "Pick a nametag color for specific players.", "Open",
                        () -> this.client.setScreen(new ChatPlayersScreen(this))));
                r.add(new Header("Screen effects"));
                r.add(new Toggle("Low fire", "Flames only cover the bottom of your screen when you burn.",
                        () -> cfg.lowFire, v -> cfg.lowFire = v));
                r.add(new Slider("Fire height", "How far the flames are lowered.", 10, 60, 5,
                        v -> String.format(java.util.Locale.ROOT, "%.2f", v / 100.0),
                        () -> cfg.lowFireOffset, v -> cfg.lowFireOffset = v));
                r.add(new Header("Screenshots"));
                r.add(new Action("Clean screenshot", "Hides the HUD; player name tags stay. Key: F9.", "Take now",
                        () -> {
                            this.client.setScreen(null);
                            CleanScreenshot.request(this.client);
                        }));
                r.add(new Note("What the clean screenshot hides (name tags always stay when kept):"));
                r.add(new Toggle("Hide the game HUD", "Hotbar, chat, crosshair ... (F1 style). Also hides all IRIS panels.",
                        () -> cfg.shotHideVanillaHud, v -> cfg.shotHideVanillaHud = v));
                r.add(new Toggle("Hide IRIS HUD panels", "Our panels (when the game HUD stays on).",
                        () -> cfg.shotHideModHud, v -> cfg.shotHideModHud = v));
                r.add(new Toggle("Hide waypoint labels", "The floating waypoint names.",
                        () -> cfg.shotHideWaypoints, v -> cfg.shotHideWaypoints = v));
                r.add(new Toggle("Hide chat bubbles", "Speech bubbles above players.",
                        () -> cfg.shotHideBubbles, v -> cfg.shotHideBubbles = v));
                r.add(new Toggle("Hide damage numbers", "Floating damage numbers.",
                        () -> cfg.shotHideDamage, v -> cfg.shotHideDamage = v));
                r.add(new Toggle("Hide light overlay", "The colored light-level squares.",
                        () -> cfg.shotHideLight, v -> cfg.shotHideLight = v));
                r.add(new Toggle("Keep player name tags", "The game hides name tags with the HUD; this keeps them.",
                        () -> cfg.shotKeepNames, v -> cfg.shotKeepNames = v));
                r.add(new Note("Keys can be changed under Options > Controls (search \"IRIS\")."));
            }
            case 6 -> {
                r.add(new Header("Damage numbers"));
                r.add(new Toggle("Damage numbers", "Numbers float up from what you hit.",
                        () -> cfg.dmgNumbers, v -> cfg.dmgNumbers = v));
                r.add(new Toggle("Only my hits", "Only creatures you were attacking. Off = all damage nearby.",
                        () -> cfg.dmgOnlyMine, v -> cfg.dmgOnlyMine = v));
                r.add(new Toggle("Show healing", "Green +numbers (needs Only my hits off).",
                        () -> cfg.dmgShowHeal, v -> cfg.dmgShowHeal = v));
                r.add(new Toggle("Decimals", "4.5 instead of rounding to whole numbers.",
                        () -> cfg.dmgDecimals, v -> cfg.dmgDecimals = v));
                r.add(new Slider("Size", "How big the numbers are.", 5, 30, 1,
                        v -> String.format(java.util.Locale.ROOT, "%.1fx", v / 10.0),
                        () -> cfg.dmgSize, v -> cfg.dmgSize = v));
                r.add(new Slider("Duration", "How long a number floats.", 5, 30, 1,
                        v -> String.format(java.util.Locale.ROOT, "%.1fs", v / 10.0),
                        () -> cfg.dmgDuration, v -> cfg.dmgDuration = v));
                r.add(new ColorRow("Color", "Auto = white, yellow, orange, red by damage.",
                        () -> cfg.dmgColor, v -> cfg.dmgColor = v));
                r.add(new Header("Hit flash"));
                r.add(new Toggle("Custom hit flash", "Recolor the red tint on creatures you hurt.",
                        () -> cfg.hitColorOn, v -> cfg.hitColorOn = v));
                r.add(new ColorRow("Flash color", "Auto = vanilla red. Rainbow and cycle animate.",
                        () -> cfg.hitColor, v -> cfg.hitColor = v));
                r.add(new Slider("Flash strength", "How strong the tint is (vanilla is 70%).", 10, 100, 5,
                        v -> v + "%", () -> cfg.hitOpacity, v -> cfg.hitOpacity = v));
                r.add(new Header("Crosshair"));
                r.add(new Cycle("Style", "Vanilla keeps the game's crosshair and attack indicator.",
                        Crosshair.STYLES, () -> cfg.crosshairStyle, v -> cfg.crosshairStyle = v));
                r.add(new Slider("Size", "Length or size of the shape.", 1, 12, 1,
                        v -> v + " px", () -> cfg.crosshairSize, v -> cfg.crosshairSize = v));
                r.add(new Slider("Thickness", "Line thickness.", 1, 4, 1,
                        v -> v + " px", () -> cfg.crosshairThickness, v -> cfg.crosshairThickness = v));
                r.add(new Slider("Gap", "Space in the middle.", 0, 8, 1,
                        v -> v + " px", () -> cfg.crosshairGap, v -> cfg.crosshairGap = v));
                r.add(new ColorRow("Color", "Auto = white.",
                        () -> cfg.crosshairColor, v -> cfg.crosshairColor = v));
                r.add(new Toggle("Outline", "A thin black edge so it shows on any background.",
                        () -> cfg.crosshairOutline, v -> cfg.crosshairOutline = v));
                r.add(new Toggle("Red on target", "Turns red while you aim at something.",
                        () -> cfg.crosshairTargetColor, v -> cfg.crosshairTargetColor = v));
                r.add(new Toggle("Hit marker", "A red X flashes when you land a hit.",
                        () -> cfg.crosshairHitMarker, v -> cfg.crosshairHitMarker = v));
                r.add(new Header("Hotbar cooldowns"));
                r.add(new Toggle("Cooldown overlay", "A colored wipe over items on cooldown.",
                        () -> cfg.cooldownOverlay, v -> cfg.cooldownOverlay = v));
                r.add(new Toggle("Seconds left", "The remaining time on the item.",
                        () -> cfg.cooldownNumbers, v -> cfg.cooldownNumbers = v));
                r.add(new Toggle("Ready flash", "A green flash when the item is usable again.",
                        () -> cfg.cooldownReadyFlash, v -> cfg.cooldownReadyFlash = v));
                r.add(new ColorRow("Overlay color", "Auto = your theme accent.",
                        () -> cfg.cooldownColor, v -> cfg.cooldownColor = v));
                r.add(new Note("Only your own screen changes: other players see nothing different."));
            }
            case 8 -> {
                r.add(new Header("Minimap"));
                r.add(new Toggle("Map", "Minimap, full map (M) and waypoints.",
                        () -> cfg.mapOn, v -> cfg.mapOn = v));
                r.add(new Slider("Minimap size", "Width and height on the screen.", 70, 200, 10,
                        v -> v + " px", () -> cfg.mapSize, v -> cfg.mapSize = v));
                r.add(new Slider("Zoom", "1 = wide view, 4 = close up. Keys: [ and ].", 1, 4, 1,
                        v -> v + "/4", () -> cfg.mapZoom, v -> cfg.mapZoom = v));
                r.add(new Toggle("Rotate with you", "Off = north is always up.",
                        () -> cfg.mapRotate, v -> cfg.mapRotate = v));
                r.add(new Toggle("Show players", "White dots for other players.",
                        () -> cfg.mapPlayers, v -> cfg.mapPlayers = v));
                r.add(new Toggle("Show mobs", "Red = hostile, green = others.",
                        () -> cfg.mapMobs, v -> cfg.mapMobs = v));
                r.add(new Cycle("Cave mode", "Shows the cave around your height. Auto = when underground.",
                        new String[]{"Off", "Auto", "Always"}, () -> cfg.mapCave, v -> cfg.mapCave = v));
                r.add(new Toggle("Coordinates", "Your position under the minimap.",
                        () -> cfg.mapCoords, v -> cfg.mapCoords = v));
                r.add(new Header("Waypoints & full map"));
                r.add(new Action("Open full map", "Pan, zoom and add waypoints. Key: M.", "Open",
                        () -> this.client.setScreen(new WorldMapScreen(this))));
                r.add(new Toggle("Waypoints on the minimap", "Colored squares.",
                        () -> cfg.mapWaypoints, v -> cfg.mapWaypoints = v));
                r.add(new Toggle("Waypoint labels in the world", "Name and distance where the waypoint is.",
                        () -> cfg.waypointLabels, v -> cfg.waypointLabels = v));
                r.add(new Toggle("Death point", "Automatically marks where you died.",
                        () -> cfg.mapDeathPoint, v -> cfg.mapDeathPoint = v));
                r.add(new Action("Share waypoints", "Friends list, inbox, and sending waypoints.", "Open",
                        () -> this.client.setScreen(new WaypointSharingScreen(this))));
                r.add(new Header("Entity radar"));
                r.add(new Toggle("Entity radar", "A movable scope with nearby entities.",
                        () -> cfg.radarOn, v -> cfg.radarOn = v));
                r.add(new Slider("Radar size", "Width and height on the screen.", 60, 160, 10,
                        v -> v + " px", () -> cfg.radarSize, v -> cfg.radarSize = v));
                r.add(new Slider("Radar range", "How far it sees.", 8, 96, 4,
                        v -> v + " blocks", () -> cfg.radarRange, v -> cfg.radarRange = v));
                r.add(new Slider("Height limit", "Hide things this far above or below you.", 4, 64, 4,
                        v -> v + " blocks", () -> cfg.radarHeight, v -> cfg.radarHeight = v));
                r.add(new Toggle("Rotate with you", "Off = north is always up.",
                        () -> cfg.radarRotate, v -> cfg.radarRotate = v));
                r.add(new Toggle("Players", "White dots, with names.",
                        () -> cfg.radarPlayers, v -> cfg.radarPlayers = v));
                r.add(new Toggle("Hostile mobs", "Red dots.",
                        () -> cfg.radarHostile, v -> cfg.radarHostile = v));
                r.add(new Toggle("Animals", "Green dots.",
                        () -> cfg.radarAnimals, v -> cfg.radarAnimals = v));
                r.add(new Toggle("Villagers and traders", "Yellow dots.",
                        () -> cfg.radarVillagers, v -> cfg.radarVillagers = v));
                r.add(new Toggle("Dropped items", "Blue dots.",
                        () -> cfg.radarItems, v -> cfg.radarItems = v));
                r.add(new Toggle("Other mobs", "Purple dots: golems, bats, fish...",
                        () -> cfg.radarOther, v -> cfg.radarOther = v));
                r.add(new Toggle("Player names", "Names under player dots.",
                        () -> cfg.radarNames, v -> cfg.radarNames = v));
                r.add(new Note("A small arrow marks things more than 4 blocks above or below you."));
                r.add(new Header("Memory"));
                r.add(new Toggle("Remember explored areas", "Saved per world / server on your computer.",
                        () -> cfg.mapSave, v -> cfg.mapSave = v));
                r.add(new Action("Clear this world's map", "Forget everything explored here.", "Clear",
                        MapData::clearCurrent));
                r.add(new Note("Keys: M full map, V add a waypoint here, [ and ] minimap zoom."));
                r.add(new Note("The map is drawn from chunks your game has loaded."));
            }
            case 9 -> {
                r.add(new Header("Emote wheel & armor"));
                if (EmoteBridge.available()) {
                    r.add(new Action("Emote menu", "Library, wheel editor and live preview. Key: N.", "Open",
                            () -> openScreen(EmoteBridge.menu(this))));
                    r.add(new Action("Emote wheel settings", "Cat ears, tail, pet, particles and more.", "Open",
                            () -> openScreen(EmoteBridge.settings(this))));
                    r.add(new Note("Hold B for the wheel. Rename armor in an anvil: Hinata or maid."));
                    r.add(new Note("Rebind under Controls > Key Binds > Emote Wheel."));
                } else {
                    r.add(new Note("The Emote Wheel part of this mod is missing from the jar."));
                }
                r.add(new Header("Chest sorter"));
                r.add(new Toggle("Sort with a key", "Press R over a chest, barrel, shulker, hopper or inventory.",
                        () -> cfg.sorterOn, v -> cfg.sorterOn = v));
                r.add(new Toggle("Include hotbar", "When sorting your own inventory.",
                        () -> cfg.sorterHotbar, v -> cfg.sorterHotbar = v));
                r.add(new Note("Sorts by item ID after merging stacks. Rebind the key under Controls."));
                r.add(new Toggle("Buttons on the screen", "Sort / Stash / Same / My inv above chests and inventory.",
                        () -> cfg.sorterButtons, v -> cfg.sorterButtons = v));
                r.add(new Toggle("Sort chests when opened", "Sorts the container automatically.",
                        () -> cfg.sorterAuto, v -> cfg.sorterAuto = v));
                r.add(new Toggle("Sort my inventory when opened", "Also sorts yours (with any chest or alone).",
                        () -> cfg.sorterAutoPlayer, v -> cfg.sorterAutoPlayer = v));
                r.add(new Note("Stash = move all your items into the chest. Same = only kinds it already holds."));
                r.add(new Header("Armor looks"));
                r.add(new Action("Armor options", "Maid / Hinata textures on or off, and show or hide armor.", "Open",
                        () -> {
                            net.minecraft.client.gui.screen.Screen s = EmoteBridge.settings(this);
                            if (s != null) {
                                this.client.setScreen(s);
                            }
                        }));
                r.add(new Note("Emote settings: Maid armor texture, Hinata armor texture, and Armor visibility."));
                r.add(new Header("Weapon Master"));
                r.add(new Toggle("Show hotbar items on my character", "Weapons and tools appear on your body.",
                        () -> cfg.wmOn, v -> cfg.wmOn = v));
                r.add(new Toggle("Swords", "On your hips.",
                        () -> cfg.wmSwords, v -> cfg.wmSwords = v));
                r.add(new Toggle("Axes, pickaxes, shovels, hoes, maces", "Crossed on your back.",
                        () -> cfg.wmTools, v -> cfg.wmTools = v));
                r.add(new Toggle("Trident", "On your back.",
                        () -> cfg.wmTrident, v -> cfg.wmTrident = v));
                r.add(new Toggle("Bows and crossbows", "On your back.",
                        () -> cfg.wmBows, v -> cfg.wmBows = v));
                r.add(new Toggle("Shield", "Flat on your back.",
                        () -> cfg.wmShield, v -> cfg.wmShield = v));
                r.add(new Toggle("Banner", "Low on your back.",
                        () -> cfg.wmBanner, v -> cfg.wmBanner = v));
                r.add(new Slider("Item size", "Scale of the displayed items.", 50, 150, 10,
                        v -> v + "%", () -> cfg.wmScale, v -> cfg.wmScale = v));
                r.add(new Toggle("Share with other players", "Players with the mod (and a server with it) see yours.",
                        () -> cfg.wmShare, v -> cfg.wmShare = v));
                r.add(new Toggle("Show other players' weapons", "Draw the loadouts they share.",
                        () -> cfg.wmShowOthers, v -> cfg.wmShowOthers = v));
                r.add(new Note("You see it in third person or freecam. Sharing needs the mod on the server."));
                r.add(new Header("Villager trade reroller"));
                r.add(new Action("Reroll for an enchantment", "Pick an enchant, min level and max price, then start.", "Open",
                        () -> this.client.setScreen(new RerollScreen(this))));
                r.add(new Note("Break and replace the job block until the trade shows up. Check server rules first."));
            }
            case 11 -> {
                r.add(new Header("Settings profiles"));
                r.add(new Action("Profiles", "Save, load and delete whole sets of settings.", "Open",
                        () -> this.client.setScreen(new ProfilesScreen(this))));
                r.add(new Note("A profile stores every setting in this mod, HUD layout included."));
                r.add(new Note("In the profile screen you can link a profile to a server or world: it switches"));
                r.add(new Note("on when you join and your normal settings come back when you leave."));
                String active = Profiles.activeServerProfile();
                if (active != null) {
                    r.add(new Note("Active server profile: " + active));
                }
            }
            case 12 -> {
                r.add(new Header("Cape and wings"));
                r.add(new Cycle("Cape", "Built-in capes, plus your own PNGs.",
                        Cosmetics.labels(true), () -> Cosmetics.indexOf(true, cfg.capeId),
                        v -> cfg.capeId = Cosmetics.idAt(true, v)));
                r.add(new Cycle("Wings", "Re-textures your elytra (shown while you wear one).",
                        Cosmetics.labels(false), () -> Cosmetics.indexOf(false, cfg.wingsId),
                        v -> cfg.wingsId = Cosmetics.idAt(false, v)));
                r.add(new Note("Your own PNGs (64x32): config/skinoverride/capes and /wings. Only you see those."));
                r.add(new Header("Seeing each other"));
                r.add(new Toggle("Share my look", "Tell players with the mod about your built-in cape and wings.",
                        () -> cfg.cosmeticsShare, v -> cfg.cosmeticsShare = v));
                r.add(new Toggle("Show other players' capes and wings", "The ones they share.",
                        () -> cfg.showOthersCosmetics, v -> cfg.showOthersCosmetics = v));
                r.add(new Note("Sharing needs this mod on the server too. Without it only you see your cosmetics."));
                r.add(new Header("Weapon Master"));
                r.add(new Toggle("Show hotbar items on my character", "Weapons and tools appear on your body.",
                        () -> cfg.wmOn, v -> cfg.wmOn = v));
                r.add(new Toggle("Share with other players", "They see your weapons too.",
                        () -> cfg.wmShare, v -> cfg.wmShare = v));
                r.add(new Toggle("Show other players' weapons", "Draw the loadouts they share.",
                        () -> cfg.wmShowOthers, v -> cfg.wmShowOthers = v));
                r.add(new Note("More Weapon Master options are in the Tools tab."));
            }
            default -> {
                r.add(new Header("GUI colors"));
                r.add(new Cycle("Preset", "Quick color schemes for every IRIS menu and HUD box.",
                        PRESET_NAMES, () -> cfg.themePreset, this::applyPreset));
                r.add(new ColorRow("Accent color", "Buttons, highlights, bars and tags.",
                        () -> cfg.themeAccent, v -> {
                            cfg.themeAccent = v;
                            cfg.themePreset = 0;
                        }, true));
                r.add(new ColorRow("Background color", "Panels and HUD boxes.",
                        () -> cfg.themeBg, v -> {
                            cfg.themeBg = v;
                            cfg.themePreset = 0;
                        }, true));
                r.add(new Slider("Panel opacity", "How see-through panels are.", 30, 100, 5,
                        v -> v + "%", () -> cfg.themeOpacity, v -> cfg.themeOpacity = v));
                r.add(new Note("Changes apply instantly to this menu, the skin screen and all HUD elements."));
            }
        }
        return r;
    }

    private void openScreen(net.minecraft.client.gui.screen.Screen screen) {
        if (screen != null) {
            this.client.setScreen(screen);
        }
    }

    private static int rowHeight(Row row) {
        if (row instanceof Header) {
            return 18;
        }
        if (row instanceof Note) {
            return 11;
        }
        if (row instanceof EmoteGrid) {
            return ((Emotes.NAMES.length + 3) / 4) * 13 + 6;
        }
        if (row instanceof ArmorGrid) {
            return 12 + 6 * 12 + 4;
        }
        return 28;
    }

    private double maxScroll() {
        int total = 4;
        for (Row r : rows()) {
            total += rowHeight(r);
        }
        return Math.max(0, total - (contentBottom - contentTop));
    }

    // ---------- setup ----------

    @Override
    protected void build() {
        pw = Math.min(this.width - 20, 430);
        ph = this.height - 24 - 34;
        px = (this.width - pw) / 2;
        py = 24;
        contentX = px + TAB_W + 8;
        contentW = pw - TAB_W - 14;
        contentTop = py + 22;
        contentBottom = py + ph - 4;

        int y = this.height - 28;
        int bw = Math.min(120, (pw - 8) / 3);
        int startX = px + (pw - (bw * 3 + 8)) / 2;
        button(startX, y, bw, 20, "Edit HUD layout", true, () -> this.client.setScreen(new HudEditorScreen(this)));
        button(startX + bw + 4, y, bw, 20, "Skins", false, () -> this.client.setScreen(new SkinOverrideScreen(this, null)));
        button(startX + (bw + 4) * 2, y, bw, 20, "Done", false, this::close);
    }

    // ---------- rendering ----------

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);

        Ui.panel(c, px, py, pw, ph);
        Ui.header(c, this.textRenderer, "IRIS'S Overrides and Tweaks  /  Settings", px + 1, py + 1, pw - 2);

        for (int i = 0; i < TABS.length; i++) {
            int ty = py + 22 + i * TAB_STEP;
            boolean sel = i == tab;
            boolean hov = Ui.inside(mouseX, mouseY, px + 4, ty, TAB_W, 15);
            Ui.roundRect(c, px + 4, ty, TAB_W, 15, sel ? Ui.ACCENT_FILL : hov ? Ui.BG_HOVER : Ui.BG_LIGHT);
            Ui.roundBorder(c, px + 4, ty, TAB_W, 15, sel ? Ui.ACCENT : Ui.BORDER);
            Ui.text(c, this.textRenderer, TABS[i], px + 12, ty + 4, sel ? 0xFFFFFFFF : Ui.TEXT);
        }

        List<Row> rows = rows();
        scroll = MathHelper.clamp(scroll, 0.0, maxScroll());
        boolean pickerOpen = picker != null;

        c.enableScissor(contentX, contentTop, contentX + contentW, contentBottom);
        int y = contentTop + 2 - (int) scroll;
        for (Row row : rows) {
            int h = rowHeight(row);
            if (y + h >= contentTop && y <= contentBottom) {
                drawRow(c, row, y, pickerOpen ? -1 : mouseX, pickerOpen ? -1 : mouseY);
            }
            y += h;
        }
        c.disableScissor();

        drawButtons(c, mouseX, mouseY);

        if (picker != null) {
            ColorPicker.draw(c, this.textRenderer, pickerX, pickerY, mouseX, mouseY, picker.get().getAsInt(), picker.plain());
        }
    }

    private void drawRow(DrawContext c, Row row, int y, int mouseX, int mouseY) {
        var tr = this.textRenderer;
        int right = contentX + contentW - 6;
        switch (row) {
            case Header h -> {
                Ui.text(c, tr, h.text(), contentX + 2, y + 6, Ui.ACCENT);
                c.fill(contentX + 2, y + 16, contentX + contentW - 4, y + 17, Ui.BORDER);
            }
            case Note n -> Ui.text(c, tr, n.text(), contentX + 2, y + 1, Ui.MUTED);
            case Toggle t -> {
                boolean hov = Ui.inside(mouseX, mouseY, contentX, y, contentW, 28);
                if (hov) {
                    Ui.roundRect(c, contentX, y, contentW - 2, 26, 0x30FFFFFF);
                }
                Ui.text(c, tr, t.label(), contentX + 6, y + 4, Ui.TEXT);
                Ui.text(c, tr, t.desc(), contentX + 6, y + 15, Ui.MUTED);
                Ui.toggle(c, right - 24, y + 7, t.get().getAsBoolean(), hov);
            }
            case Slider s -> {
                Ui.text(c, tr, s.label(), contentX + 6, y + 4, Ui.TEXT);
                Ui.text(c, tr, s.desc(), contentX + 6, y + 15, Ui.MUTED);
                int sx = right - SLIDER_W;
                int v = s.get().getAsInt();
                double frac = (v - s.min()) / (double) (s.max() - s.min());
                boolean hov = dragging == s || Ui.inside(mouseX, mouseY, sx - 4, y + 4, SLIDER_W + 8, 18);
                Ui.slider(c, sx, y + 7, SLIDER_W, frac, hov);
                String label = s.format().apply(v);
                Ui.text(c, tr, label, sx - 8 - tr.getWidth(label), y + 4, Ui.ACCENT);
            }
            case Cycle cy -> {
                boolean hov = Ui.inside(mouseX, mouseY, contentX, y, contentW, 28);
                if (hov) {
                    Ui.roundRect(c, contentX, y, contentW - 2, 26, 0x30FFFFFF);
                }
                Ui.text(c, tr, cy.label(), contentX + 6, y + 4, Ui.TEXT);
                Ui.text(c, tr, cy.desc(), contentX + 6, y + 15, Ui.MUTED);
                Ui.button(c, tr, right - 74, y + 4, 74, 18, cy.options()[cy.get().getAsInt()], hov, true, false);
            }
            case ColorRow cr -> {
                boolean hov = Ui.inside(mouseX, mouseY, contentX, y, contentW, 28);
                if (hov) {
                    Ui.roundRect(c, contentX, y, contentW - 2, 26, 0x30FFFFFF);
                }
                Ui.text(c, tr, cr.label(), contentX + 6, y + 4, Ui.TEXT);
                Ui.text(c, tr, cr.desc(), contentX + 6, y + 15, Ui.MUTED);
                Ui.roundBorder(c, right - 42, y + 5, 42, 16, hov ? Ui.ACCENT : Ui.BORDER);
                ColorPicker.swatch(c, tr, right - 41, y + 6, 40, 14, cr.get().getAsInt());
            }
            case Action a -> {
                Ui.text(c, tr, a.label(), contentX + 6, y + 4, Ui.TEXT);
                Ui.text(c, tr, a.desc(), contentX + 6, y + 15, Ui.MUTED);
                boolean hov = Ui.inside(mouseX, mouseY, right - 70, y + 4, 70, 18);
                Ui.button(c, tr, right - 70, y + 4, 70, 18, a.button(), hov, true, true);
            }
            case ArmorGrid g -> {
                int labelW = 74;
                int cell = Math.max(24, (contentW - labelW - 16) / 4);
                for (int p = 0; p < 4; p++) {
                    Ui.text(c, tr, Emotes.PIECE_NAMES[p], contentX + labelW + p * cell, y + 1, Ui.MUTED);
                }
                for (int m = 0; m < 6; m++) {
                    int ey = y + 12 + m * 12;
                    Ui.text(c, tr, Emotes.MATERIAL_LETTERS[m] + "  " + Emotes.MATERIAL_NAMES[m], contentX + 6, ey, Ui.TEXT);
                    for (int p = 0; p < 4; p++) {
                        c.drawTextWithShadow(tr, Text.literal(String.valueOf(Emotes.armorGlyph(m, p))),
                                contentX + labelW + p * cell, ey, 0xFFFFFFFF);
                    }
                }
            }
            case EmoteGrid g -> {
                int cell = (contentW - 12) / 4;
                for (int i = 0; i < Emotes.NAMES.length; i++) {
                    int ex = contentX + 6 + (i % 4) * cell;
                    int ey = y + 3 + (i / 4) * 13;
                    c.drawTextWithShadow(tr, Text.literal(String.valueOf(Emotes.glyph(i))), ex, ey, 0xFFFFFFFF);
                    Ui.text(c, tr, ":" + Emotes.NAMES[i] + ":", ex + 14, ey, Ui.MUTED);
                }
            }
        }
    }

    // ---------- input ----------

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled) || clickButtons(click)) {
            return true;
        }
        if (click.button() != 0) {
            return false;
        }
        double mx = click.x();
        double my = click.y();

        if (picker != null) {
            Integer chosen = ColorPicker.hit(mx, my, pickerX, pickerY, picker.plain());
            if (chosen != null) {
                picker.set().accept(chosen);
                cfg.save();
                clickSound();
            }
            picker = null;
            return true;
        }

        for (int i = 0; i < TABS.length; i++) {
            if (Ui.inside(mx, my, px + 4, py + 22 + i * TAB_STEP, TAB_W, 15)) {
                if (tab != i) {
                    tab = i;
                    scroll = 0;
                }
                clickSound();
                return true;
            }
        }

        if (!Ui.inside(mx, my, contentX, contentTop, contentW, contentBottom - contentTop)) {
            return false;
        }
        int y = contentTop + 2 - (int) scroll;
        int right = contentX + contentW - 6;
        for (Row row : rows()) {
            int h = rowHeight(row);
            if (my >= y && my < y + h) {
                switch (row) {
                    case Toggle t -> {
                        t.set().accept(!t.get().getAsBoolean());
                        cfg.save();
                        clickSound();
                        return true;
                    }
                    case Cycle cy -> {
                        cy.set().accept((cy.get().getAsInt() + 1) % cy.options().length);
                        cfg.save();
                        clickSound();
                        return true;
                    }
                    case Slider s -> {
                        if (mx >= right - SLIDER_W - 6) {
                            dragging = s;
                            dragX = right - SLIDER_W;
                            applySlider(s, mx);
                            return true;
                        }
                    }
                    case ColorRow cr -> {
                        picker = cr;
                        pickerX = Math.max(px + 4, right - ColorPicker.width());
                        pickerY = y + 26;
                        if (pickerY + ColorPicker.height() > py + ph - 2) {
                            pickerY = Math.max(py + 20, y - ColorPicker.height() - 2);
                        }
                        clickSound();
                        return true;
                    }
                    case Action a -> {
                        if (mx >= right - 74) {
                            clickSound();
                            a.run().run();
                            return true;
                        }
                    }
                    default -> {
                    }
                }
            }
            y += h;
        }
        return false;
    }

    private void applySlider(Slider s, double mouseX) {
        double frac = MathHelper.clamp((mouseX - dragX) / SLIDER_W, 0.0, 1.0);
        int raw = s.min() + (int) Math.round((s.max() - s.min()) * frac / s.step()) * s.step();
        s.set().accept(MathHelper.clamp(raw, s.min(), s.max()));
        Ui.applyTheme(cfg);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging != null) {
            applySlider(dragging, click.x());
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging != null) {
            dragging = null;
            cfg.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        picker = null;
        scroll = MathHelper.clamp(scroll - verticalAmount * 14, 0.0, maxScroll());
        return true;
    }

    @Override
    public void close() {
        cfg.save();
        this.client.setScreen(this.parent);
    }
}
