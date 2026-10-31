package dev.skinoverride.hud;

import dev.skinoverride.CleanScreenshot;
import dev.skinoverride.Crosshair;
import dev.skinoverride.DamageNumbers;
import dev.skinoverride.HotbarCooldowns;
import dev.skinoverride.LightOverlay;
import dev.skinoverride.ModConfig;
import dev.skinoverride.SkinOverrideClient;
import dev.skinoverride.WorldLabels;
import dev.skinoverride.map.WaypointLabels;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

import java.util.List;

/** Registers one HUD layer that draws every enabled widget at its saved position and size. */
public final class HudManager {

    public static final int PAD = 2;
    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 3.0f;

    public static final List<HudWidget> WIDGETS = List.of(
            new MinimapWidget(),
            new RadarWidget(),
            new InfoWidget(),
            new ClockWidget(),
            new KeystrokesWidget(),
            new FreecamWidget(),
            new NotificationWidget(),
            new ArmorWidget(),
            new EffectsWidget(),
            new InspectWidget(),
            new AfkWidget(),
            new SubtitleWidget(),
            new VillagerTradesWidget());

    /** Quick "hide all our HUD panels" switch (key); not saved. */
    private static boolean allHidden;

    public static boolean allHidden() {
        return allHidden;
    }

    public static void toggleAll() {
        allHidden = !allHidden;
    }

    public static boolean isHidden(HudWidget w) {
        return ModConfig.get().hudHiddenIds.contains(w.id());
    }

    public static void setHidden(HudWidget w, boolean hidden) {
        ModConfig cfg = ModConfig.get();
        cfg.hudHiddenIds.remove(w.id());
        if (hidden) {
            cfg.hudHiddenIds.add(w.id());
        }
    }

    private HudManager() {
    }

    public static void register() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CHAT,
                Identifier.of(SkinOverrideClient.MOD_ID, "hud"),
                (context, tickCounter) -> renderAll(context));

        // Our effect timers replace the vanilla effect icons while enabled.
        HudElementRegistry.replaceElement(
                VanillaHudElements.STATUS_EFFECTS,
                original -> (context, tickCounter) -> {
                    if (!ModConfig.get().effectsHud) {
                        original.render(context, tickCounter);
                    }
                });

        // Custom crosshair styles (the vanilla crosshair is still used for the "Vanilla" style).
        HudElementRegistry.replaceElement(
                VanillaHudElements.CROSSHAIR,
                original -> (context, tickCounter) ->
                        Crosshair.render(context, MinecraftClient.getInstance(), () -> original.render(context, tickCounter)));

        // Our movable subtitles replace the vanilla sound captions while enabled.
        HudElementRegistry.replaceElement(
                VanillaHudElements.SUBTITLES,
                original -> (context, tickCounter) -> {
                    if (!ModConfig.get().subtitlesHud) {
                        original.render(context, tickCounter);
                    }
                });
    }

    private static void renderAll(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) {
            return;
        }
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        ModConfig cfg = ModConfig.get();
        boolean shot = CleanScreenshot.capturing();
        boolean panelsHidden = allHidden || (shot && cfg.shotHideModHud);
        for (HudWidget w : WIDGETS) {
            if (panelsHidden || isHidden(w) || !w.enabled() || !w.visible(mc)) {
                continue;
            }
            w.prepare(mc, false);
            if (w.width() <= 0 || w.height() <= 0) {
                continue;
            }
            int[] p = position(w, sw, sh, scaledWidth(w), scaledHeight(w));
            renderWidget(context, mc, w, p[0], p[1], false);
        }
        if (!(shot && cfg.shotHideLight)) {
            LightOverlay.render(context, mc);
        }
        if (!(shot && cfg.shotHideDamage)) {
            DamageNumbers.render(context, mc);
        }
        if (!(shot && cfg.shotHideWaypoints)) {
            WaypointLabels.render(context, mc);
        }
        if (!(shot && cfg.shotHideBubbles)) {
            WorldLabels.render(context, mc);
        }
        if (!panelsHidden) {
            HotbarCooldowns.render(context, mc);
        }
    }

    // ---------- size ----------

    public static float scale(HudWidget w) {
        Float s = ModConfig.get().hudScale.get(w.id());
        return s == null ? 1f : Math.max(MIN_SCALE, Math.min(MAX_SCALE, s));
    }

    public static void setScale(HudWidget w, float scale) {
        float clamped = Math.max(MIN_SCALE, Math.min(MAX_SCALE, Math.round(scale * 10f) / 10f));
        if (Math.abs(clamped - 1f) < 0.01f) {
            ModConfig.get().hudScale.remove(w.id());
        } else {
            ModConfig.get().hudScale.put(w.id(), clamped);
        }
    }

    public static int scaledWidth(HudWidget w) {
        return Math.round(w.width() * scale(w));
    }

    public static int scaledHeight(HudWidget w) {
        return Math.round(w.height() * scale(w));
    }

    /** Draws a prepared widget with its top-left corner at (x, y), applying the widget's size setting. */
    public static void renderWidget(DrawContext context, MinecraftClient mc, HudWidget w, int x, int y, boolean preview) {
        float s = scale(w);
        if (Math.abs(s - 1f) < 0.01f) {
            w.render(context, mc, x, y, preview);
            return;
        }
        Matrix3x2fStack m = context.getMatrices();
        m.pushMatrix();
        m.translate((float) x, (float) y);
        m.scale(s, s);
        w.render(context, mc, 0, 0, preview);
        m.popMatrix();
    }

    // ---------- position ----------

    /** Top-left pixel position of a widget of the given (already scaled) size. */
    public static int[] position(HudWidget w, int sw, int sh, int ww, int wh) {
        float[] f = ModConfig.get().positions.get(w.id());
        float fx = f == null || f.length < 2 ? w.defaultX() : f[0];
        float fy = f == null || f.length < 2 ? w.defaultY() : f[1];
        int freeX = Math.max(0, sw - ww - PAD * 2);
        int freeY = Math.max(0, sh - wh - PAD * 2);
        return new int[]{PAD + Math.round(fx * freeX), PAD + Math.round(fy * freeY)};
    }

    /** Saves a widget's position from pixel coordinates (size is the already scaled size). */
    public static void setPosition(HudWidget w, int x, int y, int sw, int sh, int ww, int wh) {
        int freeX = Math.max(1, sw - ww - PAD * 2);
        int freeY = Math.max(1, sh - wh - PAD * 2);
        float fx = Math.max(0f, Math.min(1f, (x - PAD) / (float) freeX));
        float fy = Math.max(0f, Math.min(1f, (y - PAD) / (float) freeY));
        ModConfig.get().positions.put(w.id(), new float[]{fx, fy});
    }

    public static void resetLayout() {
        ModConfig.get().positions.clear();
        ModConfig.get().hudScale.clear();
        ModConfig.get().save();
    }
}
