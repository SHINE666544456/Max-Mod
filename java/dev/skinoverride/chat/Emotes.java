package dev.skinoverride.chat;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns :smile: (and optionally :) <3 ...) into small pixel emotes drawn by the mod's built-in font,
 * plus armor icons like :Dchest: (diamond chestplate) or :LLeg: (leather leggings).
 * Only YOU see these: other players need this mod too, otherwise they see the plain text.
 */
public final class Emotes {

    /** Order matches the sprite sheet (8 per row), starting at U+E000. */
    public static final String[] NAMES = {
            "smile", "grin", "laugh", "wink", "cool", "cry", "angry", "wow",
            "heart", "skull", "fire", "star", "thumbsup", "eyes", "zzz", "gg",
            "poggies"
    };

    /** Armor icons start at this sprite index (6 materials x 4 pieces). */
    public static final int ARMOR_START = 24;
    public static final String[] MATERIAL_LETTERS = {"L", "C", "I", "G", "D", "N"};
    public static final String[] MATERIAL_NAMES = {"Leather", "Chainmail", "Iron", "Gold", "Diamond", "Netherite"};
    public static final String[] PIECE_NAMES = {"Helm", "Chest", "Leg", "Boot"};

    private static final Map<String, Integer> INDEX = new HashMap<>();
    private static final Pattern NAMED = Pattern.compile(":([A-Za-z_]{2,24}):");
    private static final Pattern ARMOR_SHORT = Pattern.compile("^([lcigdn])(helm|helmet|chest|chestplate|leg|legs|leggings|boot|boots)$");
    private static final Pattern ARMOR_LONG = Pattern.compile(
            "^(leather|chain|chainmail|iron|gold|golden|diamond|netherite)_?(helmet|helm|chestplate|chest|leggings|legs|leg|boots|boot)$");
    private static final Pattern FACES = Pattern.compile(
            "(?<![^\\s])(:\\)|:D|xD|XD|;\\)|B\\)|:\\(|>:\\(|:O|:o|<3)(?![^\\s])");
    private static final Map<String, String> FACE_NAMES = new HashMap<>();

    static {
        for (int i = 0; i < NAMES.length; i++) {
            INDEX.put(NAMES[i], i);
        }
        INDEX.put("lol", 2);
        INDEX.put("love", 8);
        INDEX.put("sleep", 14);
        INDEX.put("sleepy", 14);
        INDEX.put("like", 12);
        INDEX.put("thumbs", 12);
        INDEX.put("yes", 12);
        INDEX.put("sad", 5);
        INDEX.put("mad", 6);
        INDEX.put("omg", 7);
        INDEX.put("look", 13);
        INDEX.put("dead", 9);
        INDEX.put("lit", 10);
        INDEX.put("pog", 16);
        INDEX.put("poggers", 16);
        INDEX.put("poggies", 16);

        FACE_NAMES.put(":)", "smile");
        FACE_NAMES.put(":D", "grin");
        FACE_NAMES.put("xD", "laugh");
        FACE_NAMES.put("XD", "laugh");
        FACE_NAMES.put(";)", "wink");
        FACE_NAMES.put("B)", "cool");
        FACE_NAMES.put(":(", "cry");
        FACE_NAMES.put(">:(", "angry");
        FACE_NAMES.put(":O", "wow");
        FACE_NAMES.put(":o", "wow");
        FACE_NAMES.put("<3", "heart");
    }

    private Emotes() {
    }

    public static char glyph(int index) {
        return (char) (0xE000 + index);
    }

    /** Glyph for material 0..5 (leather, chain, iron, gold, diamond, netherite) and piece 0..3 (helm, chest, leg, boot). */
    public static char armorGlyph(int material, int piece) {
        return glyph(ARMOR_START + material * 4 + piece);
    }

    private static int materialIndex(String m) {
        return switch (m) {
            case "l", "leather" -> 0;
            case "c", "chain", "chainmail" -> 1;
            case "i", "iron" -> 2;
            case "g", "gold", "golden" -> 3;
            case "d", "diamond" -> 4;
            default -> 5;
        };
    }

    private static int pieceIndex(String p) {
        if (p.startsWith("helm")) {
            return 0;
        }
        if (p.startsWith("chest")) {
            return 1;
        }
        if (p.startsWith("leg")) {
            return 2;
        }
        return 3;
    }

    /** Returns the sprite index for an emote or armor code (without the colons), or -1. */
    public static int lookup(String code) {
        String name = code.toLowerCase(Locale.ROOT);
        Integer idx = INDEX.get(name);
        if (idx != null) {
            return idx;
        }
        Matcher m = ARMOR_SHORT.matcher(name);
        if (!m.matches()) {
            m = ARMOR_LONG.matcher(name);
        }
        if (m.matches()) {
            return ARMOR_START + materialIndex(m.group(1)) * 4 + pieceIndex(m.group(2));
        }
        return -1;
    }

    public static String replace(String s, boolean faces) {
        Matcher m = NAMED.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            int idx = lookup(m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(idx < 0 ? m.group() : String.valueOf(glyph(idx))));
        }
        m.appendTail(sb);
        String result = sb.toString();
        if (faces) {
            Matcher f = FACES.matcher(result);
            StringBuilder fb = new StringBuilder();
            while (f.find()) {
                String name = FACE_NAMES.get(f.group(1));
                Integer idx = name == null ? null : INDEX.get(name);
                f.appendReplacement(fb, Matcher.quoteReplacement(idx == null ? f.group() : String.valueOf(glyph(idx))));
            }
            f.appendTail(fb);
            result = fb.toString();
        }
        return result;
    }

    /** Replaces emote words inside a Text while keeping each piece's style. */
    public static Text apply(Text text, boolean faces) {
        List<ChatColors.Seg> segs = ChatColors.segments(text);
        if (segs.isEmpty()) {
            return text;
        }
        MutableText out = Text.empty();
        boolean changed = false;
        for (ChatColors.Seg seg : segs) {
            String replaced = replace(seg.text(), faces);
            changed |= !replaced.equals(seg.text());
            out.append(Text.literal(replaced).setStyle(seg.style()));
        }
        return changed ? out : text;
    }
}
