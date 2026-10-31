package dev.skinoverride;

import dev.skinoverride.chat.ChatColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Custom crosshair styles, red-on-target and a hit marker. Style "Vanilla" leaves the game's crosshair alone. */
public final class Crosshair {

    public static final String[] STYLES = {"Vanilla", "Dot", "Plus", "Plus + dot", "Circle", "Square", "X"};
    private static final int HIT_MARKER_MS = 260;

    private Crosshair() {
    }

    public static void render(DrawContext c, MinecraftClient mc, Runnable vanilla) {
        ModConfig cfg = ModConfig.get();
        if (mc.player == null || mc.options.hudHidden || mc.currentScreen != null
                || !mc.options.getPerspective().isFirstPerson()) {
            if (cfg.crosshairStyle == 0) {
                vanilla.run();
            }
            return;
        }
        int cx = mc.getWindow().getScaledWidth() / 2;
        int cy = mc.getWindow().getScaledHeight() / 2;

        if (cfg.crosshairStyle == 0) {
            vanilla.run();
        } else {
            int rgb = baseColor(cfg);
            if (cfg.crosshairTargetColor && mc.targetedEntity != null) {
                rgb = 0xFF5555;
            }
            int color = 0xFF000000 | rgb;
            if (cfg.crosshairOutline) {
                int black = 0xFF000000;
                shape(c, cfg, cx - 1, cy, black);
                shape(c, cfg, cx + 1, cy, black);
                shape(c, cfg, cx, cy - 1, black);
                shape(c, cfg, cx, cy + 1, black);
            }
            shape(c, cfg, cx, cy, color);
        }

        long age = System.currentTimeMillis() - DamageNumbers.lastHitMs;
        if (cfg.crosshairHitMarker && age >= 0 && age < HIT_MARKER_MS) {
            int alpha = (int) (255 * (1.0 - age / (double) HIT_MARKER_MS));
            int color = Ui.withAlpha(0xFF4040, Math.max(alpha, 30));
            for (int i = 4; i <= 8; i++) {
                c.fill(cx - i, cy - i, cx - i + 1, cy - i + 1, color);
                c.fill(cx + i, cy - i, cx + i + 1, cy - i + 1, color);
                c.fill(cx - i, cy + i, cx - i + 1, cy + i + 1, color);
                c.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, color);
            }
        }
    }

    private static int baseColor(ModConfig cfg) {
        int mode = cfg.crosshairColor;
        if (mode == ChatColors.DEFAULT) {
            return 0xFFFFFF;
        }
        if (mode == ChatColors.RAINBOW) {
            return ChatColors.hsv((System.currentTimeMillis() % 3000L) / 3000f, 0.7f, 1f);
        }
        if (mode == ChatColors.CYCLE) {
            return ChatColors.cycleColor();
        }
        return mode;
    }

    private static void rect(DrawContext c, int x, int y, int w, int h, int color) {
        if (w > 0 && h > 0) {
            c.fill(x, y, x + w, y + h, color);
        }
    }

    private static void shape(DrawContext c, ModConfig cfg, int cx, int cy, int color) {
        int s = cfg.crosshairSize;
        int t = cfg.crosshairThickness;
        int g = cfg.crosshairGap;
        int halfT = t / 2;
        switch (cfg.crosshairStyle) {
            case 1 -> rect(c, cx - s / 2, cy - s / 2, s, s, color);
            case 2, 3 -> {
                rect(c, cx - halfT, cy - g - s, t, s, color);
                rect(c, cx - halfT, cy + g, t, s, color);
                rect(c, cx - g - s, cy - halfT, s, t, color);
                rect(c, cx + g, cy - halfT, s, t, color);
                if (cfg.crosshairStyle == 3) {
                    rect(c, cx - halfT, cy - halfT, t, t, color);
                }
            }
            case 4 -> {
                int outer = s + g;
                int inner = Math.max(0, outer - t);
                for (int dy = -outer; dy <= outer; dy++) {
                    int ox = (int) Math.floor(Math.sqrt((double) outer * outer - (double) dy * dy));
                    if (Math.abs(dy) < inner) {
                        int ix = (int) Math.floor(Math.sqrt((double) inner * inner - (double) dy * dy));
                        rect(c, cx - ox, cy + dy, ox - ix, 1, color);
                        rect(c, cx + ix + 1, cy + dy, ox - ix, 1, color);
                    } else {
                        rect(c, cx - ox, cy + dy, ox * 2 + 1, 1, color);
                    }
                }
            }
            case 5 -> {
                int h = s + g;
                rect(c, cx - h, cy - h, h * 2 + 1, t, color);
                rect(c, cx - h, cy + h - t + 1, h * 2 + 1, t, color);
                rect(c, cx - h, cy - h, t, h * 2 + 1, color);
                rect(c, cx + h - t + 1, cy - h, t, h * 2 + 1, color);
            }
            case 6 -> {
                for (int i = 0; i < s; i++) {
                    int d = g + i;
                    rect(c, cx - d, cy - d, t, t, color);
                    rect(c, cx + d, cy - d, t, t, color);
                    rect(c, cx - d, cy + d, t, t, color);
                    rect(c, cx + d, cy + d, t, t, color);
                }
            }
            default -> {
            }
        }
    }
}
