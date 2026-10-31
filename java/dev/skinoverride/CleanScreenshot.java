package dev.skinoverride;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;

/**
 * Takes a screenshot with the HUD hidden. What hides and what stays is up to you (Settings -> Visuals):
 * the vanilla HUD, our HUD panels, waypoint labels, chat bubbles, damage numbers and the light overlay each have
 * a switch. Player name tags are kept: the game normally hides them together with the HUD (F1), so while the
 * shot is taken they are let through (a mixin on MinecraftClient.isHudEnabled).
 */
public final class CleanScreenshot {

    private static int ticksLeft;
    private static boolean hudWasHidden;

    private CleanScreenshot() {
    }

    /** True while a clean screenshot is being prepared (the last few frames before it is saved). */
    public static boolean capturing() {
        return ticksLeft > 0;
    }

    public static void request(MinecraftClient mc) {
        if (ticksLeft > 0 || mc.currentScreen != null || mc.player == null) {
            return;
        }
        hudWasHidden = mc.options.hudHidden;
        if (ModConfig.get().shotHideVanillaHud) {
            mc.options.hudHidden = true;
        }
        ticksLeft = 3; // let a few frames render without the HUD
    }

    public static void tick(MinecraftClient mc) {
        if (ticksLeft <= 0) {
            return;
        }
        if (mc.currentScreen != null) {
            mc.options.hudHidden = hudWasHidden;
            ticksLeft = 0;
            return;
        }
        if (--ticksLeft == 0) {
            ScreenshotRecorder.saveScreenshot(mc.runDirectory, mc.getFramebuffer(),
                    message -> mc.execute(() -> mc.inGameHud.getChatHud().addMessage(message)));
            mc.options.hudHidden = hudWasHidden;
        }
    }
}
