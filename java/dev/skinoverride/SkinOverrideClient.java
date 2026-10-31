package dev.skinoverride;

import dev.skinoverride.hud.HudManager;
import dev.skinoverride.hud.MinimapWidget;
import dev.skinoverride.map.MapData;
import dev.skinoverride.map.SharedWaypoints;
import dev.skinoverride.map.Waypoints;
import dev.skinoverride.net.CosmeticSync;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/** Client entrypoint: keybinds, HUD layer and the AFK tracker. */
public class SkinOverrideClient implements ClientModInitializer {

    public static final String MOD_ID = "skinoverride";
    public static final Logger LOGGER = LoggerFactory.getLogger("IRIS'S Overrides and Tweaks");

    private static KeyBinding skinsKey;
    private static KeyBinding settingsKey;
    private static KeyBinding zoomKey;
    private static KeyBinding fullbrightKey;
    private static KeyBinding shotKey;
    private static KeyBinding freecamKey;
    private static KeyBinding lightKey;
    private static KeyBinding mapKey;
    private static KeyBinding waypointKey;
    private static KeyBinding zoomInKey;
    private static KeyBinding zoomOutKey;
    private static KeyBinding acceptWpKey;
    private static KeyBinding sharePosKey;
    private static KeyBinding chatSearchKey;
    private static KeyBinding caveKey;
    private static KeyBinding radarKey;
    private static KeyBinding rerollKey;
    private static KeyBinding hudToggleKey;

    @Override
    public void onInitializeClient() {
        SkinStore.init();
        ModConfig.get();
        HudManager.register();
        CosmeticSync.init();
        TradeReroller.registerCallbacks();

        skinsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skinoverride.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, KeyBinding.Category.MISC));
        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skinoverride.settings", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, KeyBinding.Category.MISC));

        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skinoverride.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, KeyBinding.Category.MISC));
        fullbrightKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skinoverride.fullbright", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, KeyBinding.Category.MISC));
        shotKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skinoverride.cleanshot", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F9, KeyBinding.Category.MISC));

        freecamKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skinoverride.freecam", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8, KeyBinding.Category.MISC));
        lightKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skinoverride.light", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F7, KeyBinding.Category.MISC));

        mapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skinoverride.map", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M, KeyBinding.Category.MISC));
        waypointKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skinoverride.waypoint", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, KeyBinding.Category.MISC));
        zoomInKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skinoverride.mapzoomin", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, KeyBinding.Category.MISC));
        zoomOutKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skinoverride.mapzoomout", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, KeyBinding.Category.MISC));
        KeyBinding sortKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skinoverride.sort", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, KeyBinding.Category.MISC));
        ChestSorter.setKey(sortKey);

        acceptWpKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skinoverride.acceptwp", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Y, KeyBinding.Category.MISC));
        SharedWaypoints.acceptKey = acceptWpKey;
        sharePosKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skinoverride.sharepos", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, KeyBinding.Category.MISC));
        chatSearchKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skinoverride.chatsearch", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, KeyBinding.Category.MISC));
        caveKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skinoverride.cave", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, KeyBinding.Category.MISC));
        radarKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skinoverride.radar", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, KeyBinding.Category.MISC));
        hudToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skinoverride.hudtoggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, KeyBinding.Category.MISC));
        rerollKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skinoverride.reroll", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, KeyBinding.Category.MISC));

        // Weapon Master: draw your hotbar weapons on your own character.
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, helper, context) -> {
            if (entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
                helper.register(new WeaponMasterFeature(playerRenderer));
            }
        });

        // Freecam must grab the movement keys before the game reads them.
        ClientTickEvents.START_CLIENT_TICK.register(Freecam::startTick);

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            Profiles.shutdown();
            Fullbright.restore();
            PlayTime.save();
            MapData.saveNow(true);
            Waypoints.save();
        });

        // Day counter, clock and playtime on the pause menu.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof GameMenuScreen) {
                ScreenEvents.afterRender(screen).register((s, context, mouseX, mouseY, tickDelta) ->
                        PauseInfo.render(context, client, tickDelta));
            }
        });

        // Sort / Stash buttons, trade-reroll buttons and the chat search button.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ChestSorter.onScreenInit(client, screen);
            TradeReroller.onScreenInit(client, screen);
            if (screen instanceof ChatScreen) {
                Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("Search"),
                                b -> client.setScreen(new ChatSearchScreen(screen)))
                        .dimensions(scaledWidth - 48, scaledHeight - 34, 44, 14).build());
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Profiles.tick(client);
            TradeReroller.tick(client);
            VillagerTrades.tick(client);
            CosmeticSync.tick(client);
            AfkTracker.tick(client);
            DayClock.tick(client);
            Subtitles.ensureRegistered(client);
            Zoom.tick(client, zoomKey.isPressed());
            Fullbright.tick(client);
            CleanScreenshot.tick(client);
            Notifications.tick(client);
            LightOverlay.tick(client);
            DamageNumbers.tick(client);
            HitColor.tick(client);
            MapData.tick(client);
            ChestSorter.tick(client);

            while (mapKey.wasPressed()) {
                if (client.world != null && client.currentScreen == null) {
                    client.setScreen(new WorldMapScreen(null));
                }
            }
            while (waypointKey.wasPressed()) {
                if (client.player != null && client.world != null) {
                    int n = Waypoints.list().size() + 1;
                    Waypoints.add("Waypoint " + n, client.player.getBlockX(), client.player.getBlockY(), client.player.getBlockZ());
                    client.inGameHud.setOverlayMessage(Text.literal("Waypoint " + n + " added"), false);
                }
            }
            while (zoomInKey.wasPressed()) {
                ModConfig cfg = ModConfig.get();
                cfg.mapZoom = Math.min(4, cfg.mapZoom + 1);
                cfg.save();
                client.inGameHud.setOverlayMessage(Text.literal(MinimapWidget.zoomText()), false);
            }
            while (zoomOutKey.wasPressed()) {
                ModConfig cfg = ModConfig.get();
                cfg.mapZoom = Math.max(1, cfg.mapZoom - 1);
                cfg.save();
                client.inGameHud.setOverlayMessage(Text.literal(MinimapWidget.zoomText()), false);
            }

            while (acceptWpKey.wasPressed()) {
                if (client.player != null && !SharedWaypoints.acceptLatest(client)) {
                    client.inGameHud.setOverlayMessage(Text.literal("No shared waypoint waiting"), false);
                }
            }
            while (sharePosKey.wasPressed()) {
                if (client.player != null && client.world != null) {
                    Waypoints.Waypoint here = new Waypoints.Waypoint();
                    here.name = client.player.getName().getString() + "'s spot";
                    here.x = client.player.getBlockX();
                    here.y = client.player.getBlockY();
                    here.z = client.player.getBlockZ();
                    if (SharedWaypoints.send(client, here, null)) {
                        client.inGameHud.setOverlayMessage(Text.literal("Position sent to chat"), false);
                    }
                }
            }
            while (chatSearchKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new ChatSearchScreen(null));
                }
            }
            while (caveKey.wasPressed()) {
                ModConfig cfg = ModConfig.get();
                cfg.mapCave = (cfg.mapCave + 1) % 3;
                cfg.save();
                if (client.player != null) {
                    String[] names = {"off", "automatic", "always on"};
                    client.inGameHud.setOverlayMessage(Text.literal("Cave map " + names[cfg.mapCave]), false);
                }
            }
            while (radarKey.wasPressed()) {
                ModConfig cfg = ModConfig.get();
                cfg.radarOn = !cfg.radarOn;
                cfg.save();
                if (client.player != null) {
                    client.inGameHud.setOverlayMessage(Text.literal("Radar " + (cfg.radarOn ? "on" : "off")), false);
                }
            }
            while (hudToggleKey.wasPressed()) {
                HudManager.toggleAll();
                if (client.player != null) {
                    client.inGameHud.setOverlayMessage(Text.literal(HudManager.allHidden()
                            ? "IRIS HUD panels hidden" : "IRIS HUD panels shown"), false);
                }
            }
            while (rerollKey.wasPressed()) {
                if (TradeReroller.active()) {
                    TradeReroller.stop(client, "Reroll stopped");
                } else if (client.currentScreen == null && client.world != null) {
                    client.setScreen(new RerollScreen(null));
                }
            }

            while (freecamKey.wasPressed()) {
                Freecam.toggle(client);
            }
            while (lightKey.wasPressed()) {
                ModConfig cfg = ModConfig.get();
                cfg.lightOverlay = !cfg.lightOverlay;
                cfg.save();
                if (client.player != null) {
                    client.inGameHud.setOverlayMessage(Text.literal("Light overlay " + (cfg.lightOverlay ? "on" : "off")), false);
                }
            }

            while (fullbrightKey.wasPressed()) {
                ModConfig cfg = ModConfig.get();
                cfg.fullbright = !cfg.fullbright;
                cfg.save();
                if (client.player != null) {
                    client.inGameHud.setOverlayMessage(Text.literal("Fullbright " + (cfg.fullbright ? "on" : "off")), false);
                }
            }
            while (shotKey.wasPressed()) {
                CleanScreenshot.request(client);
            }

            while (skinsKey.wasPressed()) {
                // If you're looking at a player, open the GUI with them preselected.
                UUID target = client.targetedEntity instanceof AbstractClientPlayerEntity p ? p.getUuid() : null;
                client.setScreen(new SkinOverrideScreen(client.currentScreen, target));
            }
            while (settingsKey.wasPressed()) {
                client.setScreen(new SettingsScreen(client.currentScreen));
            }
        });
    }
}
