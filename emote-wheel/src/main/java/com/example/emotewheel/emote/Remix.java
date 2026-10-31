package com.example.emotewheel.emote;

import static com.example.emotewheel.emote.Anim.*;

/**
 * Rewrites of emotes that used to be near-copies of each other (found by comparing their motion).
 * Each one now has its own body language, not just a different arm angle.
 */
final class Remix {
    private Remix() {}

    static void apply() {
        // ---------------- the "one arm forward" family
        Emotes.remix("point_forward", (p, t) -> {          // determined lunge, head scanning ahead
            p.rightArm(-1.5708f - sin(t * 3) * 0.05f, 0.05f, 0); p.leftArm(0.25f, 0, -0.35f);
            p.rightLeg(-0.35f, 0, 0.05f); p.leftLeg(0.3f, 0, -0.05f); p.rootY = 1.2f; p.lean = 0.2f;
            p.head(-0.2f, sin(t * 1.2f) * 0.2f, 0);
        });
        Emotes.remix("point_you", (p, t) -> {              // accusing jabs, other hand on hip
            float jab = pos(sin(t * 5));
            p.rightArm(-1.45f - jab * 0.12f, -0.1f, 0); p.leftArm(0.1f, 0, -0.9f);
            p.lean = 0.12f + jab * 0.15f; p.head(0.05f - jab * 0.1f, 0, 0); p.legs(0, 0, 0.18f); p.bodyY = -0.2f * jab;
        });
        Emotes.remix("fist_bump", (p, t) -> {              // reach out, bump, pull back, little hop
            float ph = saw(t, 1.6f);
            p.rax = seq(ph, 0f, -0.6f, 0.35f, -1.55f, 0.5f, -1.65f, 0.9f, -0.6f, 1.6f, -0.6f);
            float bump = clamp01((-p.rax - 0.6f) / 1.0f);
            p.lean = 0.2f * bump; p.rootY = -bump * 1.2f; p.headX = 0.1f * bump; p.leftArm(0.3f, 0, -0.3f);
        });
        Emotes.remix("knock_knock", (p, t) -> {            // knock three times, then lean in to listen
            float ph = saw(t, 3.2f);
            float knock = ph < 1.0f ? pos(sin(ph * 3 * TAU)) : 0f;
            float listen = smooth((ph - 1.2f) / 0.4f) * (1 - smooth((ph - 2.6f) / 0.4f));
            p.rightArm(-1.7f + knock * 0.12f, 0.1f, 0); p.lean = 0.05f + 0.2f * listen;
            p.head(0.05f - knock * 0.06f, 0.4f * listen, 0.25f * listen); p.leftArm(0.1f, 0, -0.3f);
        });
        Emotes.remix("handshake", (p, t) -> {              // firm two-beat pump with a polite bow
            float s = sin(t * 6.5f);
            p.rightArm(-1.4f + s * 0.25f, 0.12f, 0); p.leftArm(0.5f, 0, -0.25f); p.lean = 0.18f + s * 0.04f;
            p.head(0.15f + s * 0.06f, 0, 0); p.legs(0, 0, 0.1f);
        });
        Emotes.remix("thumbs_up", (p, t) -> {              // cocked arm, wink tilt, bouncy
            float b = abs(sin(t * 3.2f));
            p.rightArm(-2.15f + sin(t * 14) * 0.03f * b, 0.3f, 0); p.leftArm(0.1f, 0, -0.8f);
            p.headZ = 0.2f; p.rootY = -b * 0.9f; p.legs(0, 0, 0.1f);
        });
        Emotes.remix("thumbs_down", (p, t) -> {            // low and unimpressed, slow head shake
            p.rightArm(-0.9f, 0.4f, 0.2f); p.leftArm(-1.0f, -0.2f, 0);
            p.head(0.2f, sin(t * 2) * 0.35f, 0); p.lean = -0.1f;
        });
        Emotes.remix("rps", (p, t) -> {                    // three shakes, then the reveal
            float ph = saw(t, 3.4f);
            float shake = ph < 1.8f ? sin(ph / 1.8f * 3 * TAU) : 0f;
            float reveal = smooth((ph - 1.8f) / 0.2f);
            p.rax = -1.4f + shake * 0.35f - 0.3f * reveal; p.ray = 0.1f; p.rootY = -pos(shake) * 0.6f;
            p.lean = 0.15f * reveal; p.head(0.05f, 0, 0.18f * reveal); p.leftArm(0.2f, 0, -0.4f);
        });
        Emotes.remix("beckon", (p, t) -> {                 // two-handed "come on", leaning back
            float s = sin(t * 6.5f);
            p.rightArm(-1.6f + s * 0.4f, 0.1f, 0); p.leftArm(-1.3f + sin(t * 6.5f + PI) * 0.3f, -0.1f, 0);
            p.lean = -0.12f; p.headZ = 0.15f; p.rootY = 0.8f; p.legs(0, 0, 0.12f);
        });
        Emotes.remix("tea_pour", (p, t) -> {               // a slow, careful pour
            float pour = (1 - cos(t * 0.9f)) / 2f;
            p.rightArm(-1.35f - 0.25f * pour, 0.15f, 0.12f * pour); p.leftArm(-1.15f, 0.25f, 0);
            p.lean = 0.12f; p.headX = 0.2f + 0.15f * pour; p.legs(0, 0, 0.05f);
        });
        Emotes.remix("salute_two", (p, t) -> {             // crisp: snaps up with overshoot
            p.rightArm(seq(t, 0f, 0f, 0.12f, -2.7f, 0.3f, -2.5f), -0.2f, -0.1f); p.leftArm(0.1f, 0, -0.1f);
            p.lean = -0.08f; p.head(-0.3f, 0, 0);
        });
        Emotes.remix("wipe_sweat", (p, t) -> {             // swipe across the forehead, breathing hard
            p.rightArm(-2.5f, sin(t * 3.3f) * 0.35f - 0.25f, 0); p.head(-0.25f, 0, 0); p.lean = 0.12f;
            p.rootY = sin(t * 4f) * 0.5f;
        });
        Emotes.remix("shush", (p, t) -> {                  // finger to lips, glancing around
            p.rightArm(-2.35f, -0.18f, 0); p.leftArm(-0.5f, 0.3f, -0.4f); p.lean = 0.1f;
            p.head(0.05f, sin(t * 1.4f) * 0.5f, 0); p.bodyY = sin(t * 1.4f) * 0.15f;
        });
        Emotes.remix("cheers", (p, t) -> {                 // raise, clink forward, sip
            float ph = saw(t, 2.4f);
            p.rightArm(seq(ph, 0f, -2.0f, 0.5f, -1.55f, 0.9f, -2.0f, 1.5f, -2.4f, 2.0f, -2.0f, 2.4f, -2.0f), -0.1f, 0);
            p.headX = -0.15f * smooth((ph - 1.2f) / 0.3f) * (1 - smooth((ph - 1.9f) / 0.3f)); p.leftArm(0.05f, 0, -0.1f);
        });
        Emotes.remix("scheming", (p, t) -> {               // hand-rubbing, slow evil head tilts
            p.rax = -1.2f + sin(t * 10) * 0.15f; p.lax = -1.2f - sin(t * 10) * 0.15f; p.ray = -0.5f; p.lay = 0.5f;
            p.lean = 0.25f; p.head(0.35f, sin(t * 0.9f) * 0.4f, 0.2f * sin(t * 0.5f));
        });
        Emotes.remix("pray", (p, t) -> {                   // kneel and pray
            p.rootY = 8; p.rightLeg(-1.25f, 0, 0.1f); p.leftLeg(1.25f, 0, -0.1f);
            p.arms(-1.15f, -0.62f, 0); p.head(0.5f + sin(t * 1.1f) * 0.04f, 0, 0); p.lean = 0.12f;
        });
        Emotes.remix("sigh", (p, t) -> {                   // shoulders rise, then the whole body sags
            float e = env(t, 0.9f, 0.5f, 1.0f);
            float rise = sin(PI * clamp01(t / 1.0f));
            p.lean = 0.3f * e; p.headX = 0.2f * e; p.arms(0.4f * rise - 0.1f * e, 0, 0.12f * e); p.headY = sin(t * 2) * 0.1f * e;
        });
        Emotes.remix("disappointed", (p, t) -> {           // slow, sad head shake
            p.head(0.5f, sin(t * 1.5f) * 0.4f, 0); p.lean = 0.15f; p.arms(0.2f, 0, 0.1f);
        });
        Emotes.remix("sleepy", (p, t) -> {                 // nods off... and jerks awake
            float ph = saw(t, 3.6f);
            float droop = ph < 2.6f ? ph / 2.6f * 0.75f : lerp(0.75f, 0f, smooth((ph - 2.6f) / 0.15f));
            p.headX = droop; p.lean = 0.08f + 0.1f * droop; p.arms(0.1f, 0, 0.1f); p.rootX = sin(t * 1.2f) * 0.5f;
        });
        Emotes.remix("standing_sleep", (p, t) -> {         // swaying like a tree in the wind
            float sway = sin(t * 0.6f);
            p.lean = 0.1f + 0.15f * sway * sway; p.head(0.5f, 0, 0.2f * sway); p.arms(sin(t * 0.9f) * 0.15f, 0, 0.1f);
            p.rootX = sway * 1.2f;
        });
        Emotes.remix("proud", (p, t) -> {                  // hands on lapels, chest out, rocking on heels
            p.arms(-0.6f, -0.6f, 0.35f); p.lean = -0.15f; p.head(-0.25f, 0, 0);
            p.rootY = abs(sin(t * 1.6f)) * -0.5f; p.rootX = sin(t * 0.8f) * 0.5f;
        });

        // ---------------- the "hands up near the face" anime family
        Emotes.remix("simple_domain", (p, t) -> {          // a dome of hands overhead, hovering
            p.arms(-2.7f, -0.25f, 0.3f); p.rootY = -2f + sin(t * 1.6f); p.spin = sin(t * 0.4f) * 0.25f;
            p.head(-0.3f, 0, 0); p.legs(0, 0, 0.04f);
        });
        Emotes.remix("ten_shadows", (p, t) -> {            // low, fast shadow-puppet hands
            p.rax = -0.9f + sin(t * 9) * 0.12f; p.lax = -0.9f + sin(t * 9 + PI) * 0.12f; p.ray = -0.55f; p.lay = 0.55f;
            p.lean = 0.2f; p.head(0.3f, 0, 0); p.legs(0, 0, 0.1f); p.bodyY = sin(t * 4.5f) * 0.1f;
        });
        Emotes.remix("six_eyes", (p, t) -> {               // hand at the blindfold, chin up, floating
            p.rightArm(-2.55f, -0.15f, 0); p.leftArm(0.05f, 0, -0.08f); p.head(-0.25f, 0, 0.1f);
            p.lean = -0.1f; p.rootY = -1.5f + sin(t * 1.6f) * 0.6f;
        });
        Emotes.remix("cursed_speech", (p, t) -> {          // fingers to the lips, then a commanding thrust
            float ph = saw(t, 3f);
            float cmd = smooth((ph - 1.2f) / 0.15f) * (1 - smooth((ph - 1.9f) / 0.3f));
            p.rightArm(-2.3f, -0.3f, 0); p.leftArm(lerp(0.1f, -1.57f, cmd), 0.15f * cmd, 0);
            p.head(-0.2f - 0.15f * cmd, 0, 0); p.lean = -0.15f + 0.35f * cmd;
        });
        Emotes.remix("spirit_gun", (p, t) -> {             // finger gun with a real recoil
            float ph = saw(t, 1.6f);
            float recoil = seq(ph, 0f, 0f, 0.9f, 0f, 0.95f, 0.45f, 1.1f, 0f);
            p.rax = -1.5708f + recoil; p.leftArm(-1.3f, 0.3f, 0); p.lean = -0.12f * recoil / 0.45f;
            p.rootX = recoil * -0.8f; p.legs(0, 0, 0.2f);
        });
        Emotes.remix("zoltraak", (p, t) -> {               // wind up in a spiral, then blast
            float ph = saw(t, 3f);
            float blast = smooth((ph - 2.0f) / 0.12f) * (1 - smooth((ph - 2.6f) / 0.3f));
            p.rightArm(-1.6f, sin(ph * TAU * 1.5f) * 0.35f * (1 - blast), 0); p.leftArm(0.3f, 0, -0.5f);
            p.lean = 0.4f * blast; p.body(0, 0.5f * blast, 0); p.legs(0, 0, 0.25f);
        });
        Emotes.remix("star_finger", (p, t) -> {            // extended lunge, arm shaking at the target
            p.rightArm(-1.5708f, 0.1f + pos(sin(t * 7.5f)) * 0.05f, 0); p.leftArm(0.6f, 0, -0.9f);
            p.lean = 0.35f; p.rightLeg(-0.5f, 0, 0.05f); p.leftLeg(0.4f, 0, -0.05f); p.rootY = 1.8f; p.head(-0.35f, 0, 0);
        });
    }
}
