package dev.skinoverride;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;

import java.lang.reflect.Constructor;

/**
 * Opens the screens of the nested Emote Wheel mod. That mod is built with Mojang's names while this one uses
 * Yarn names, so the two cannot share compiled code; at runtime they are the same game, so reflection works.
 */
public final class EmoteBridge {

    private static final String SCREENS = "com.example.emotewheel.client.screen.";

    private EmoteBridge() {
    }

    public static boolean available() {
        return FabricLoader.getInstance().isModLoaded("emote_wheel");
    }

    /** The Emote Wheel settings (cat ears, tail, pet, particles ...). */
    public static Screen settings(Screen parent) {
        return build("EmoteSettingsScreen", new Class<?>[]{Screen.class}, parent);
    }

    /** The emote library / wheel editor. */
    public static Screen menu(Screen parent) {
        return build("EmoteMenuScreen", new Class<?>[]{Screen.class, int.class}, parent, 0);
    }

    private static Screen build(String name, Class<?>[] types, Object... args) {
        if (!available()) {
            return null;
        }
        try {
            Constructor<?> constructor = Class.forName(SCREENS + name).getConstructor(types);
            return (Screen) constructor.newInstance(args);
        } catch (ReflectiveOperationException | RuntimeException e) {
            SkinOverrideClient.LOGGER.warn("Could not open the Emote Wheel screen {}", name, e);
            return null;
        }
    }
}
