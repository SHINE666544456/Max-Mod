package com.example.emotewheel.emote;

import static com.example.emotewheel.emote.Anim.*;
import static com.example.emotewheel.emote.Category.MAID;

/** More cat and maid emotes. Everything in the MAID category also shows cat ears and a tail. */
final class CatMaidEmotes {
    private CatMaidEmotes() {}

    static void register() {
        Emotes.add("paw_bow", "Paw Bow", MAID, "cod", 2.8f, (p, t) -> {
            float e = env(t, 0.7f, 1.0f, 0.9f);
            p.lean = 0.8f * e; p.arms(-1.0f * e, 0, 0.1f * e); p.headX = -0.4f * e;
        });
        Emotes.add("cat_scratch", "Scratch Scratch", MAID, "iron_nugget", 0, (p, t) -> {
            p.rax = -1.3f + sin(t * 12) * 0.45f; p.lax = -1.3f + sin(t * 12 + PI) * 0.45f; p.ray = -0.15f; p.lay = 0.15f;
            p.lean = 0.15f; p.headX = 0.1f;
        });
        Emotes.add("cat_stalk", "Cat Stalk", MAID, "string", 0, (p, t) -> {
            float s = sin(t * 3);
            p.lean = 0.85f; p.arms(-0.5f, 0, 0.15f); p.rlx = s * 0.5f; p.llx = -s * 0.5f; p.rootY = 2.5f;
            p.headX = -0.6f; p.rootX = s * 0.6f;
        });
        Emotes.add("cat_groom", "Paw Wash", MAID, "milk_bucket", 0, (p, t) -> {
            p.rightArm(-2.4f, -0.2f + sin(t * 8) * 0.15f, 0); p.leftArm(0.05f, 0, -0.1f); p.head(0.3f, 0, 0.2f);
            p.lean = 0.1f;
        });
        Emotes.add("tail_chase", "Chase Your Tail", MAID, "lead", 0, (p, t) -> {
            p.spin = t * 6f; p.rootY = 4; p.legs(-legAngle(4), 0, 0.15f); p.arms(0.3f, 0, 0.4f); p.lean = 0.2f;
        });
        Emotes.add("bunny_kick", "Bunny Kick", MAID, "rabbit_foot", 0, (p, t) -> {
            p.flip = -HP; p.rootY = 10; p.rlx = -0.4f + sin(t * 10) * 0.8f; p.llx = -0.4f - sin(t * 10) * 0.8f;
            p.arms(-1.2f, -0.3f, 0);
        });
        Emotes.add("tea_pour", "Pour Tea", MAID, "potion", 0, (p, t) -> {
            p.rightArm(-1.45f + sin(t * 2) * 0.15f, 0.1f, 0); p.leftArm(0.5f, 0, -0.3f); p.lean = 0.15f; p.headX = 0.1f;
        });
        Emotes.add("maid_polish", "Polish", MAID, "sponge", 0, (p, t) -> {
            p.rightArm(-1.2f + cos(t * 6) * 0.2f, sin(t * 6) * 0.35f, 0); p.leftArm(-1.2f + cos(t * 6) * 0.2f, sin(t * 6) * 0.35f, 0);
            p.lean = 0.25f; p.headX = -0.1f;
        });
        Emotes.add("apron_twirl", "Apron Twirl", MAID, "white_banner", 0, (p, t) -> {
            p.spin = t * 5f; p.arms(0.2f, 0, 0.55f); p.rootY = -abs(sin(t * 2.5f)) * 1.0f;
        });
        Emotes.add("moe_moe_kyun", "Moe Moe Kyun!", MAID, "pink_tulip", 0, (p, t) -> {
            p.arms(-1.7f, -0.55f + sin(t * 4) * 0.08f, 0); p.head(0.05f, 0, 0.25f);
            p.rootY = -abs(sin(t * 2)) * 1.0f; p.legs(0, 0, 0.08f);
        });
        Emotes.add("formal_bow", "Formal Bow", MAID, "name_tag", 3.0f, (p, t) -> {
            float e = env(t, 0.8f, 1.1f, 0.9f);
            p.lean = 0.9f * e; p.arms(-0.3f * e, -0.45f * e, 0); p.headX = -0.2f * e;
        });
        Emotes.add("see_you_master", "See You, Master!", MAID, "cake", 0, (p, t) -> {
            p.rax = -2.75f; p.raz = -0.3f + sin(t * 8) * 0.45f; p.headZ = 0.2f + sin(t * 8) * 0.05f; p.leftArm(0.1f, 0, -0.2f);
            p.rootY = -abs(sin(t * 4)) * 0.8f;
        });
        Emotes.add("cat_ears_pose", "Cat Pose", MAID, "cod", 0, (p, t) -> {
            p.arms(-2.4f, -0.4f, 0); p.head(0.0f, 0, 0.25f); p.legs(0, 0, 0.15f); p.bodyY = sin(t * 2) * 0.15f;
        });
        Emotes.add("neko_run", "Neko Run", MAID, "leather_boots", 0, (p, t) -> {
            float s = sin(t * 15);
            p.lean = 0.7f; p.arms(-1.0f + s * 0.3f, 0, 0.1f); p.rlx = s * 1.1f; p.llx = -s * 1.1f; p.rootY = -abs(s) * 1.0f;
            p.headX = -0.5f;
        });
    }
}
