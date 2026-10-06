package com.example.emotewheel.emote;

import static com.example.emotewheel.emote.Anim.*;
import static com.example.emotewheel.emote.Category.ANIME;

import com.example.emotewheel.States;

/** Toggle emotes that only exist for the owner account (see States.OWNER). */
final class OwnerEmotes {
    private OwnerEmotes() {}

    static void register() {
        // Turn into the block you hold in your off hand (and back).
        Emotes.add("substitution_jutsu", "Substitution Jutsu", ANIME, "oak_log", 1.5f, (p, t) -> {
            float seal = env(t, 0.2f, 0.45f, 0.2f);
            p.rightArm(lerp(0f, -1.45f, seal), lerp(0f, -0.75f, seal), 0);
            p.leftArm(lerp(0f, -1.6f, seal), lerp(0f, 0.75f, seal), 0);
            p.spin = TAU * smooth((t - 0.6f) / 0.5f);
            p.rootY = -sin(PI * clamp01((t - 0.6f) / 0.6f)) * 3f;
            p.headX = 0.1f * seal;
        }).state(States.SUBSTITUTION, 10).only(States.OWNER);

        // Hide your nametag and locator-bar dot (and show them again).
        Emotes.add("stealth_mode", "Vanish", ANIME, "black_dye", 1.0f, (p, t) -> {
            float e = env(t, 0.2f, 0.5f, 0.25f);
            p.rightArm(-2.35f * e, -0.18f * e, 0);
            p.headY = sin(t * 7f) * 0.4f * e; p.lean = 0.1f * e; p.legs(0, 0, 0.05f * e);
        }).state(States.STEALTH, 5).only(States.OWNER);
    }
}
