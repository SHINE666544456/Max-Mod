package dev.skinoverride.chat;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Color choices for chat (solid, rainbow, cycling) and helpers that restyle Text without losing click/hover events. */
public final class ChatColors {

    /** Leave the original color alone. */
    public static final int DEFAULT = -1;
    /** Every letter gets the next color of the rainbow. */
    public static final int RAINBOW = -2;
    /** The whole text gets one color that shifts over time (each new message is a different color). */
    public static final int CYCLE = -3;

    public record Swatch(String name, int rgb) {
    }

    public static final Swatch[] PALETTE = {
            new Swatch("White", 0xFFFFFF), new Swatch("Silver", 0xC0C0C0),
            new Swatch("Gray", 0x808080), new Swatch("Charcoal", 0x555555),
            new Swatch("Red", 0xFF5555), new Swatch("Crimson", 0xAA0000),
            new Swatch("Orange", 0xFFAA00), new Swatch("Gold", 0xFFD700),
            new Swatch("Yellow", 0xFFFF55), new Swatch("Lime", 0xA6FF4D),
            new Swatch("Green", 0x55FF55), new Swatch("Forest", 0x00AA00),
            new Swatch("Mint", 0x98FFB3), new Swatch("Aqua", 0x55FFFF),
            new Swatch("Teal", 0x00AAAA), new Swatch("Sky", 0x7FC8FF),
            new Swatch("Blue", 0x5555FF), new Swatch("Navy", 0x0000AA),
            new Swatch("Lavender", 0xB69CFF), new Swatch("Purple", 0xAA00AA),
            new Swatch("Magenta", 0xFF55FF), new Swatch("Pink", 0xFF9EC8),
            new Swatch("Coral", 0xFF7F7F), new Swatch("Brown", 0xC08040),
            new Swatch("Sakura", 0xFFB7D5), new Swatch("Rose", 0xFF4FA3),
            new Swatch("Blush", 0xFFD1E3)
    };

    private ChatColors() {
    }

    public static String describe(int mode) {
        if (mode == DEFAULT) {
            return "Auto (unchanged)";
        }
        if (mode == RAINBOW) {
            return "Rainbow";
        }
        if (mode == CYCLE) {
            return "Color cycle";
        }
        for (Swatch s : PALETTE) {
            if (s.rgb() == mode) {
                return s.name();
            }
        }
        return String.format("#%06X", mode & 0xFFFFFF);
    }

    public static int hsv(float h, float s, float v) {
        float hh = (h - (float) Math.floor(h)) * 6f;
        int i = (int) hh;
        float f = hh - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        float r;
        float g;
        float b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    public static int cycleColor() {
        return hsv((System.currentTimeMillis() % 9000L) / 9000f, 0.6f, 1f);
    }

    // ---------- text helpers ----------

    public record Seg(Style style, String text) {
    }

    /** Flattens a Text into styled pieces (styles already merged with their parents). */
    public static List<Seg> segments(Text text) {
        List<Seg> out = new ArrayList<>();
        text.<Object>visit((style, str) -> {
            if (!str.isEmpty()) {
                out.add(new Seg(style, str));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    /** True for the private-use characters our emote font uses. */
    public static boolean isEmote(int codePoint) {
        return codePoint >= 0xE000 && codePoint <= 0xE0FF;
    }

    /** Applies a color mode to a Text. Emote glyphs stay uncolored so they keep their own colors. */
    public static Text apply(Text src, int mode) {
        if (mode == DEFAULT) {
            return src;
        }
        List<Seg> segs = segments(src);
        if (segs.isEmpty()) {
            return src;
        }
        MutableText out = Text.empty();
        float start = (System.currentTimeMillis() % 6000L) / 6000f;
        int solid = mode == CYCLE ? cycleColor() : mode;
        int letter = 0;
        for (Seg seg : segs) {
            for (int cp : seg.text().codePoints().toArray()) {
                String ch = new String(Character.toChars(cp));
                Style style = seg.style();
                if (isEmote(cp)) {
                    style = style.withColor(0xFFFFFF);
                } else if (!Character.isWhitespace(cp)) {
                    int rgb = mode == RAINBOW ? hsv(start + letter * 0.06f, 0.65f, 1f) : solid;
                    style = style.withColor(rgb);
                    letter++;
                }
                out.append(Text.literal(ch).setStyle(style));
            }
        }
        return out;
    }
}
