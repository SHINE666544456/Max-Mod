package com.example.emotewheel.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;

import java.lang.reflect.Field;

/**
 * The look of "Iris's Overrides" for the emote screens. The colors are read live from the main mod's
 * theme (Settings -> Theme), so changing the theme there restyles the emote menu, wheel and settings too.
 * If the main mod is not present the default teal theme is used.
 */
public final class IrisTheme {

    private static final String UI_CLASS = "dev.skinoverride.Ui";
    private static final String[] NAMES = {
        "ACCENT", "ACCENT_DIM", "ACCENT_FILL", "ACCENT_FILL_HOVER", "BG", "BG_LIGHT", "BG_HOVER",
        "BORDER", "HEADER_TOP", "TEXT", "MUTED", "GOOD", "BAD", "WARN"
    };
    private static final int[] V = {
        0xFF35D0BA, 0xFF1D7266, 0xFF207D70, 0xFF29A291, 0xE0111420, 0xE01F222D, 0xE0193D42,
        0xFF2F4D51, 0xC01C4C4E, 0xFFE8ECF4, 0xFF8A93A8, 0xFF56D364, 0xFFFF5555, 0xFFFFC857
    };
    private static final int I_ACCENT = 0, I_ACCENT_DIM = 1, I_ACCENT_FILL = 2, I_ACCENT_FILL_HOVER = 3, I_BG = 4,
        I_BG_LIGHT = 5, I_BG_HOVER = 6, I_BORDER = 7, I_HEADER_TOP = 8, I_TEXT = 9, I_MUTED = 10, I_GOOD = 11,
        I_BAD = 12, I_WARN = 13;

    private static Field[] fields;
    private static boolean tried;
    private static long last;

    private IrisTheme() {
    }

    /** Re-reads the colors from the main mod (cheap, at most every 50 ms). */
    public static void refresh() {
        long now = System.nanoTime();
        if (tried && now - last < 50_000_000L) {
            return;
        }
        last = now;
        if (!tried) {
            tried = true;
            try {
                Class<?> ui = Class.forName(UI_CLASS);
                Field[] f = new Field[NAMES.length];
                for (int i = 0; i < NAMES.length; i++) {
                    f[i] = ui.getField(NAMES[i]);
                }
                fields = f;
            } catch (ReflectiveOperationException | RuntimeException e) {
                fields = null;
            }
        }
        if (fields != null) {
            try {
                for (int i = 0; i < fields.length; i++) {
                    V[i] = fields[i].getInt(null);
                }
            } catch (IllegalAccessException | RuntimeException e) {
                fields = null;
            }
        }
    }

    public static int accent() { return V[I_ACCENT]; }
    public static int accentDim() { return V[I_ACCENT_DIM]; }
    public static int accentFill() { return V[I_ACCENT_FILL]; }
    public static int accentFillHover() { return V[I_ACCENT_FILL_HOVER]; }
    public static int bg() { return V[I_BG]; }
    public static int bgLight() { return V[I_BG_LIGHT]; }
    public static int bgHover() { return V[I_BG_HOVER]; }
    public static int border() { return V[I_BORDER]; }
    public static int headerTop() { return V[I_HEADER_TOP]; }
    public static int text() { return V[I_TEXT]; }
    public static int muted() { return V[I_MUTED]; }
    public static int good() { return V[I_GOOD]; }
    public static int bad() { return V[I_BAD]; }
    public static int warn() { return V[I_WARN]; }

    // ---------- color math ----------

    public static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0xFFFFFF);
    }

    public static int accentA(int alpha) { return withAlpha(accent(), alpha); }
    public static int bgA(int alpha) { return withAlpha(bg(), alpha); }
    public static int mutedA(int alpha) { return withAlpha(muted(), alpha); }

    /** Blend of two colors (alpha is dropped). */
    public static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    // ---------- drawing ----------

    public static void roundRect(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x + 1, y, x + w - 1, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    public static void roundBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x + 1, y, x + w - 1, y + 1, color);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        refresh();
        roundRect(g, x, y, w, h, bg());
        roundBorder(g, x, y, w, h, border());
    }

    /** A soft title strip with an accent underline, like the main mod's headers. */
    public static void header(GuiGraphics g, int x, int y, int w) {
        refresh();
        int top = headerTop();
        int alpha = (top >>> 24) & 0xFF;
        for (int row = 0; row < 16; row++) {
            int a = Math.round(alpha * (1f - row / 16f));
            g.fill(x, y + row, x + w, y + row + 1, withAlpha(top, a));
        }
        g.fill(x, y + 15, x + w, y + 16, accentDim());
        g.fill(x, y + 15, x + Math.min(w, 28), y + 16, accent());
    }

    /** Draws the themed frame over a text box (call after the screen's widgets were drawn). */
    public static void frame(GuiGraphics g, EditBox box) {
        refresh();
        int x = box.getX();
        int y = box.getY();
        int w = box.getWidth();
        int h = box.getHeight();
        int color = box.isFocused() ? accent() : border();
        g.fill(x + 1, y, x + w - 1, y + 1, color);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
        int corner = 0xFF000000 | (bg() & 0xFFFFFF);
        g.fill(x, y, x + 1, y + 1, corner);
        g.fill(x + w - 1, y, x + w, y + 1, corner);
        g.fill(x, y + h - 1, x + 1, y + h, corner);
        g.fill(x + w - 1, y + h - 1, x + w, y + h, corner);
    }
}
