package dev.skinoverride;

import dev.skinoverride.chat.ChatColors;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A small grid of color swatches drawn and hit-tested by hand.
 * Full mode: auto, rainbow, cycle + 24 colors (chat). Plain mode: just the 24 colors (GUI theme).
 */
public final class ColorPicker {

    public static final int CELL = 16;
    public static final int COLS = 9;
    public static final int PAD = 4;

    private static final List<Integer> FULL = new ArrayList<>();
    private static final List<Integer> PLAIN = new ArrayList<>();

    static {
        FULL.add(ChatColors.DEFAULT);
        FULL.add(ChatColors.RAINBOW);
        FULL.add(ChatColors.CYCLE);
        for (ChatColors.Swatch s : ChatColors.PALETTE) {
            FULL.add(s.rgb());
            PLAIN.add(s.rgb());
        }
    }

    private ColorPicker() {
    }

    private static List<Integer> values(boolean plain) {
        return plain ? PLAIN : FULL;
    }

    public static int width() {
        return COLS * CELL + PAD * 2;
    }

    /** Height of the full picker (the plain one is never taller). */
    public static int height() {
        return ((FULL.size() + COLS - 1) / COLS) * CELL + PAD * 2 + 12;
    }

    public static void draw(DrawContext c, TextRenderer tr, int x, int y, int mouseX, int mouseY, int selected) {
        draw(c, tr, x, y, mouseX, mouseY, selected, false);
    }

    public static void draw(DrawContext c, TextRenderer tr, int x, int y, int mouseX, int mouseY,
                            int selected, boolean plain) {
        List<Integer> values = values(plain);
        Ui.panel(c, x, y, width(), height());
        String hoverName = null;
        for (int i = 0; i < values.size(); i++) {
            int cx = x + PAD + (i % COLS) * CELL;
            int cy = y + PAD + (i / COLS) * CELL;
            int v = values.get(i);
            swatch(c, tr, cx + 1, cy + 1, CELL - 2, CELL - 2, v);
            if (v == selected) {
                Ui.roundBorder(c, cx, cy, CELL, CELL, 0xFFFFFFFF);
            }
            if (Ui.inside(mouseX, mouseY, cx, cy, CELL, CELL)) {
                Ui.roundBorder(c, cx, cy, CELL, CELL, Ui.ACCENT);
                hoverName = ChatColors.describe(v);
            }
        }
        String label = hoverName != null ? hoverName : ChatColors.describe(selected);
        c.drawTextWithShadow(tr, label, x + PAD + 1, y + height() - 12, hoverName != null ? Ui.TEXT : Ui.MUTED);
    }

    public static Integer hit(double mx, double my, int x, int y) {
        return hit(mx, my, x, y, false);
    }

    /** Returns the chosen value, or null if the click was not on a swatch. */
    public static Integer hit(double mx, double my, int x, int y, boolean plain) {
        List<Integer> values = values(plain);
        for (int i = 0; i < values.size(); i++) {
            int cx = x + PAD + (i % COLS) * CELL;
            int cy = y + PAD + (i / COLS) * CELL;
            if (Ui.inside(mx, my, cx, cy, CELL, CELL)) {
                return values.get(i);
            }
        }
        return null;
    }

    /** One color preview box. */
    public static void swatch(DrawContext c, TextRenderer tr, int x, int y, int w, int h, int mode) {
        if (mode == ChatColors.DEFAULT) {
            Ui.roundRect(c, x, y, w, h, 0xFF2A3042);
            String a = "A";
            c.drawTextWithShadow(tr, a, x + (w - tr.getWidth(a)) / 2, y + (h - 8) / 2, Ui.MUTED);
        } else if (mode == ChatColors.RAINBOW) {
            int stripes = Math.max(3, w / 3);
            for (int i = 0; i < stripes; i++) {
                int x0 = x + i * w / stripes;
                int x1 = x + (i + 1) * w / stripes;
                c.fill(x0, y, x1, y + h, 0xFF000000 | ChatColors.hsv(i / (float) stripes, 0.7f, 1f));
            }
        } else if (mode == ChatColors.CYCLE) {
            c.fillGradient(x, y, x + w, y + h, 0xFFB69CFF, 0xFF35D0BA);
        } else {
            c.fill(x, y, x + w, y + h, 0xFF000000 | mode);
        }
    }
}
