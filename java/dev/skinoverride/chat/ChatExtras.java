package dev.skinoverride.chat;

import dev.skinoverride.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Chat extras: a timestamp in front of every line, a sound and highlight when someone says your name
 * (or one of your extra words), and a searchable history of everything you saw this session.
 */
public final class ChatExtras {

    /** One remembered chat line. */
    public record Line(String time, String text, boolean mention) {
    }

    private static final int MAX_LINES = 1500;
    private static final List<Line> HISTORY = new ArrayList<>();
    private static boolean mentionFlag;

    private ChatExtras() {
    }

    // ---------- history ----------

    public static void clear() {
        HISTORY.clear();
    }

    public static int size() {
        return HISTORY.size();
    }

    /** Newest first. Every word of the query must appear (case-insensitive). */
    public static List<Line> search(String query, boolean mentionsOnly) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String[] words = q.isEmpty() ? new String[0] : q.split("\\s+");
        List<Line> out = new ArrayList<>();
        for (int i = HISTORY.size() - 1; i >= 0; i--) {
            Line line = HISTORY.get(i);
            if (mentionsOnly && !line.mention()) {
                continue;
            }
            String low = line.text().toLowerCase(Locale.ROOT);
            boolean all = true;
            for (String w : words) {
                if (!low.contains(w)) {
                    all = false;
                    break;
                }
            }
            if (all) {
                out.add(line);
            }
        }
        return out;
    }

    // ---------- timestamps ----------

    public static String stamp(ModConfig cfg) {
        String pattern;
        if (cfg.chatTime24h) {
            pattern = cfg.chatTimeSeconds ? "HH:mm:ss" : "HH:mm";
        } else {
            pattern = cfg.chatTimeSeconds ? "h:mm:ss a" : "h:mm a";
        }
        return LocalTime.now().format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH));
    }

    /** Called with every finished chat line: records it and adds the timestamp. */
    public static Text finish(Text processed) {
        ModConfig cfg = ModConfig.get();
        String stamp = stamp(cfg);
        String plain = processed.getString();
        if (plain.length() > 500) {
            plain = plain.substring(0, 500);
        }
        HISTORY.add(new Line(stamp, plain, mentionFlag));
        mentionFlag = false;
        while (HISTORY.size() > MAX_LINES) {
            HISTORY.remove(0);
        }
        if (!cfg.chatTimestamps) {
            return processed;
        }
        return Text.empty()
                .append(Text.literal("[" + stamp + "] ").formatted(Formatting.GRAY))
                .append(processed);
    }

    // ---------- mentions ----------

    private static boolean sameName(String sender, String self) {
        for (String token : sender.split("[^A-Za-z0-9_]+")) {
            if (token.equalsIgnoreCase(self)) {
                return true;
            }
        }
        return false;
    }

    /** True if the message (written by someone else) contains your name or one of your extra words. */
    public static boolean isMention(String sender, String message) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.chatMention) {
            return false;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return false;
        }
        String self = mc.player.getName().getString();
        if (sameName(sender, self)) {
            return false;
        }
        List<String> words = new ArrayList<>();
        words.add(self);
        for (String w : cfg.chatMentionWords.split(",")) {
            String t = w.trim();
            if (t.length() >= 2) {
                words.add(t);
            }
        }
        for (String w : words) {
            Pattern p = Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(w) + "(?![A-Za-z0-9_])",
                    Pattern.CASE_INSENSITIVE);
            if (p.matcher(message).find()) {
                return true;
            }
        }
        return false;
    }

    /** Marks the line being processed as a mention and plays the sound. */
    public static void noteMention() {
        mentionFlag = true;
        playSound(MinecraftClient.getInstance(), ModConfig.get());
    }

    public static void playSound(MinecraftClient mc, ModConfig cfg) {
        if (!cfg.chatMentionSound || mc.getSoundManager() == null) {
            return;
        }
        switch (cfg.chatMentionSoundChoice) {
            case 1 -> mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.4f, 0.8f));
            case 2 -> mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.0f));
            default -> mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2f, 1.0f));
        }
    }
}
