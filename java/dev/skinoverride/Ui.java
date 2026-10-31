package dev.skinoverride;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;

/** Shared look: dark panels, an accent color and slightly rounded corners. Colors follow the Theme settings. */
public final class Ui {

    // theme colors (recomputed by applyTheme)
    public static int ACCENT = 0xFF35D0BA;
    public static int ACCENT_DIM = 0xFF1E7F72;
    public static int ACCENT_FILL = 0xFF1E8F80;
    public static int ACCENT_FILL_HOVER = 0xFF2FB7A3;
    public static int BG = 0xE0111420;
    public static int BG_LIGHT = 0xE01B2132;
    public static int BG_HOVER = 0xE0283149;
    public static int BORDER = 0xFF2B3347;
    public static int HEADER_TOP = 0xC0213047;

    public static final int TEXT = 0xFFE8ECF4;
    public static final int MUTED = 0xFF8A93A8;
    public static final int GOOD = 0xFF56D364;
    public static final int BAD = 0xFFFF5555;
    public static final int WARN = 0xFFFFC857;

    private Ui() {
    }

    // ---------- color math ----------

    public static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    public static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    /** Recomputes every theme color from the settings. */
    public static void applyTheme(ModConfig cfg) {
        int accent = cfg.themeAccent & 0xFFFFFF;
        int bg = cfg.themeBg & 0xFFFFFF;
        int alpha = Math.max(30, Math.min(100, cfg.themeOpacity)) * 255 / 100;

        ACCENT = 0xFF000000 | accent;
        ACCENT_DIM = 0xFF000000 | mix(accent, 0x000000, 0.45f);
        ACCENT_FILL = 0xFF000000 | mix(accent, 0x000000, 0.40f);
        ACCENT_FILL_HOVER = 0xFF000000 | mix(accent, 0x000000, 0.22f);
        BG = withAlpha(bg, alpha);
        BG_LIGHT = withAlpha(mix(bg, 0xFFFFFF, 0.06f), alpha);
        BG_HOVER = withAlpha(mix(bg, accent, 0.22f), alpha);
        BORDER = 0xFF000000 | mix(mix(bg, 0xFFFFFF, 0.12f), accent, 0.18f);
        HEADER_TOP = withAlpha(mix(bg, accent, 0.30f), 0xC0);
    }

    // ---------- drawing ----------

    public static void roundRect(DrawContext c, int x, int y, int w, int h, int color) {
        c.fill(x + 1, y, x + w - 1, y + h, color);
        c.fill(x, y + 1, x + 1, y + h - 1, color);
        c.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    public static void roundBorder(DrawContext c, int x, int y, int w, int h, int color) {
        c.fill(x + 1, y, x + w - 1, y + 1, color);
        c.fill(x + 1, y + h - 1, x + w - 1, y + h, color);
        c.fill(x, y + 1, x + 1, y + h - 1, color);
        c.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    public static void panel(DrawContext c, int x, int y, int w, int h) {
        roundRect(c, x, y, w, h, BG);
        roundBorder(c, x, y, w, h, BORDER);
    }

    public static void card(DrawContext c, int x, int y, int w, int h, int border) {
        roundRect(c, x, y, w, h, BG_LIGHT);
        roundBorder(c, x, y, w, h, border);
    }

    /** Title strip with a soft gradient and an accent underline. */
    public static void header(DrawContext c, TextRenderer tr, String title, int x, int y, int w) {
        c.fillGradient(x, y, x + w, y + 16, HEADER_TOP, HEADER_TOP & 0x00FFFFFF);
        c.fill(x, y + 15, x + w, y + 16, ACCENT_DIM);
        c.fill(x, y + 15, x + Math.min(w, 28), y + 16, ACCENT);
        text(c, tr, title, x + 6, y + 4, TEXT);
    }

    public static void button(DrawContext c, TextRenderer tr, int x, int y, int w, int h,
                              String label, boolean hovered, boolean enabled, boolean primary) {
        int fill;
        int border;
        int textColor;
        if (!enabled) {
            fill = 0xA0161A26;
            border = 0xFF222838;
            textColor = 0xFF5A6277;
        } else if (primary) {
            fill = hovered ? ACCENT_FILL_HOVER : ACCENT_FILL;
            border = ACCENT;
            textColor = 0xFFFFFFFF;
        } else {
            fill = hovered ? BG_HOVER : BG_LIGHT;
            border = hovered ? ACCENT_DIM : BORDER;
            textColor = TEXT;
        }
        roundRect(c, x, y, w, h, fill);
        roundBorder(c, x, y, w, h, border);
        String shown = tr.trimToWidth(label, w - 6);
        c.drawTextWithShadow(tr, shown, x + (w - tr.getWidth(shown)) / 2, y + (h - 8) / 2, textColor);
    }

    public static void toggle(DrawContext c, int x, int y, boolean on, boolean hovered) {
        roundRect(c, x, y, 24, 12, on ? ACCENT_DIM : 0xFF2A3042);
        roundBorder(c, x, y, 24, 12, on ? ACCENT : (hovered ? MUTED : BORDER));
        int knobX = on ? x + 14 : x + 2;
        roundRect(c, knobX, y + 2, 8, 8, on ? 0xFFFFFFFF : 0xFF9AA3B8);
    }

    public static void slider(DrawContext c, int x, int y, int w, double frac, boolean hovered) {
        int trackY = y + 5;
        roundRect(c, x, trackY, w, 4, 0xFF2A3042);
        int fillW = (int) Math.round(w * Math.max(0, Math.min(1, frac)));
        if (fillW > 0) {
            roundRect(c, x, trackY, Math.max(2, fillW), 4, ACCENT);
        }
        int knobX = x + fillW - 3;
        roundRect(c, knobX, y + 2, 6, 10, hovered ? 0xFFFFFFFF : 0xFFD6DCEA);
    }

    public static void text(DrawContext c, TextRenderer tr, String s, int x, int y, int color) {
        c.drawTextWithShadow(tr, s, x, y, color);
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** True for half of every 600 ms: used for blinking warnings. */
    public static boolean blink() {
        return (Util.getMeasuringTimeMs() / 300L) % 2L == 0L;
    }

    public static String formatSeconds(int total) {
        if (total < 0) {
            total = 0;
        }
        int h = total / 3600;
        int m = (total % 3600) / 60;
        int s = total % 60;
        if (h > 0) {
            return h + ":" + (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
        }
        return m + ":" + (s < 10 ? "0" : "") + s;
    }

    /** Short form for AFK times: 45s, 3m, 1h 05m. */
    public static String formatShort(int total) {
        if (total < 60) {
            return total + "s";
        }
        if (total < 3600) {
            return (total / 60) + "m";
        }
        int m = (total % 3600) / 60;
        return (total / 3600) + "h " + (m < 10 ? "0" : "") + m + "m";
    }
}
