package dev.skinoverride;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Listens to every sound the game plays and remembers the ones that have a subtitle. */
public final class Subtitles {

    public static final int LIFETIME_MS = 3000;
    private static final int MAX_ENTRIES = 12;

    private static final class Entry {
        final Text text;
        final String key;
        Vec3d pos;
        float range;
        long time;

        Entry(Text text, Vec3d pos, float range, long time) {
            this.text = text;
            this.key = text.getString();
            this.pos = pos;
            this.range = range;
            this.time = time;
        }
    }

    public record Line(Text text, Vec3d pos, float range, long ageMs) {
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static boolean registered;

    private Subtitles() {
    }

    public static void ensureRegistered(MinecraftClient mc) {
        if (registered || mc.getSoundManager() == null) {
            return;
        }
        mc.getSoundManager().registerListener(Subtitles::onSound);
        registered = true;
    }

    private static void onSound(SoundInstance sound, WeightedSoundSet soundSet, float range) {
        Text subtitle = soundSet.getSubtitle();
        if (subtitle == null) {
            return;
        }
        Vec3d pos = new Vec3d(sound.getX(), sound.getY(), sound.getZ());
        long now = System.currentTimeMillis();
        String key = subtitle.getString();
        synchronized (ENTRIES) {
            for (Entry e : ENTRIES) {
                if (e.key.equals(key)) {
                    e.pos = pos;
                    e.range = range;
                    e.time = now;
                    return;
                }
            }
            ENTRIES.add(new Entry(subtitle, pos, range, now));
            while (ENTRIES.size() > MAX_ENTRIES) {
                ENTRIES.remove(0);
            }
        }
    }

    /** Current (not yet expired) subtitle lines, oldest first. */
    public static List<Line> snapshot() {
        long now = System.currentTimeMillis();
        List<Line> out = new ArrayList<>();
        synchronized (ENTRIES) {
            ENTRIES.removeIf(e -> now - e.time > LIFETIME_MS);
            for (Entry e : ENTRIES) {
                out.add(new Line(e.text, e.pos, e.range, now - e.time));
            }
        }
        return out;
    }
}
