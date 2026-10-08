package com.example.emotewheel.emote;

import static com.example.emotewheel.emote.Anim.*;

/**
 * A small "personality" added on top of every emote so even similar poses feel different:
 * breathing, weight shifts, head wander, and a category-flavoured entrance. Everything is derived from the
 * emote's id, so each emote always has the same personality, but no two share it.
 */
public final class Flavor {
    private Flavor() {}

    public static void apply(Pose p, Emote e, float t) {
        int h = e.id().hashCode();
        float ph1 = (h & 0xFF) / 255f * TAU;
        float ph2 = ((h >> 8) & 0xFF) / 255f * TAU;
        float ph3 = ((h >> 16) & 0xFF) / 255f * TAU;
        float f1 = 0.75f + ((h >>> 24) & 0x7F) / 127f * 0.9f;   // 0.75 .. 1.65
        float f2 = 0.6f + ((h >> 5) & 0x7F) / 127f * 0.9f;      // 0.6 .. 1.5

        Category c = e.category();
        float amp = (c == Category.MOOD || c == Category.POSE || c == Category.MAID) ? 1.0f
                  : (c == Category.DANCE || c == Category.ACTION) ? 0.4f : 0.7f;

        // breathing and weight shift
        p.rootY += sin(t * 1.25f * f1 + ph1) * 0.16f * amp;
        p.rootX += sin(t * 0.55f * f2 + ph2) * 0.35f * amp;
        p.bodyY += sin(t * 0.6f * f2 + ph3) * 0.05f * amp;
        // head wanders a little
        p.headY += sin(t * 0.8f * f1 + ph2) * 0.10f * amp;
        p.headX += sin(t * 1.0f * f2 + ph3) * 0.035f * amp;
        p.headZ += sin(t * 0.5f * f1 + ph1) * 0.04f * amp;
        // arms never hang perfectly still
        p.rax += sin(t * 1.1f * f2 + ph1) * 0.035f * amp;
        p.lax += sin(t * 1.1f * f2 + ph3 + 1.7f) * 0.035f * amp;

        // category-flavoured entrance (0 -> 1 -> 0 over the first third of a second)
        float entrance = sin(PI * clamp01(t / 0.35f));
        switch (c) {
            case ANIME -> { p.lean -= 0.10f * entrance; p.rootY -= 0.9f * entrance; } // a dramatic rise before the hold
            case ACTION -> p.rootY += 0.8f * entrance;                               // brace
            case GREET -> p.headX += 0.12f * entrance;                               // a little nod
            case MOOD -> { p.lean += 0.08f * entrance; p.headX += 0.10f * entrance; } // a slump
            case MAID -> { p.headZ += 0.10f * entrance; p.rootY += 0.3f * entrance; } // a curtsy-ish dip
            case DANCE -> p.rootY -= abs(sin(t * PI * (1.6f + (h & 3) * 0.25f))) * 0.35f; // each dance gets its own bounce
            case SILLY -> p.headZ += sin(t * 2.3f * f1 + ph2) * 0.08f;
            default -> { }
        }
    }
}
