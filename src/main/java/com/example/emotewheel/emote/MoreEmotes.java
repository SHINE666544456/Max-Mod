package com.example.emotewheel.emote;

import static com.example.emotewheel.emote.Anim.*;
import static com.example.emotewheel.emote.Category.*;

/** Sports, music and everyday gestures. */
final class MoreEmotes {
    private MoreEmotes() {}

    static void register() {
        // ---------------- sports
        Emotes.add("basketball_dribble", "Dribble", ACTION, "orange_wool", 0, (p, t) -> {
            p.rax = -0.7f + sin(t * 9) * 0.45f; p.ray = -0.1f; p.lean = 0.15f; p.legs(0, 0, 0.2f);
            p.rootY = (1 - cos(t * 9)) * 0.3f; p.lax = 0.2f;
        });
        Emotes.add("basketball_shoot", "Jump Shot", ACTION, "slime_block", 0, (p, t) -> {
            float ph = saw(t, 2.0f);
            p.arms(seq(ph, 0f, -1.2f, 0.5f, -2.7f, 0.8f, -2.3f, 1.2f, -1.2f, 2f, -1.2f), -0.1f, 0);
            p.rootY = -seq(ph, 0f, 0f, 0.5f, 0f, 0.8f, 4f, 1.2f, 0f);
            p.legs(-0.3f * smooth(-p.rootY / 4f), 0, 0.1f);
        });
        Emotes.add("soccer_kick", "Soccer Kick", ACTION, "leather_boots", 0, (p, t) -> {
            float ph = saw(t, 1.6f);
            p.rlx = seq(ph, 0f, 0.6f, 0.5f, 0.9f, 0.65f, -1.5f, 1.0f, -0.4f, 1.6f, 0.6f);
            p.lean = -0.2f * pos(-p.rlx / 1.5f); p.arms(0, 0, 0.9f);
        });
        Emotes.add("golf_swing", "Golf Swing", ACTION, "iron_hoe", 0, (p, t) -> {
            float ph = saw(t, 2.4f);
            float yaw = seq(ph, 0f, 0f, 1.0f, -0.7f, 1.4f, 0.9f, 2.0f, 0.5f, 2.4f, 0f);
            p.rightArm(-1.1f, yaw * 0.9f, 0); p.leftArm(-1.1f, yaw * 0.9f, 0); p.bodyY = yaw * 0.6f;
            p.lean = 0.35f; p.headX = -0.3f; p.legs(0, 0, 0.2f);
        });
        Emotes.add("baseball_swing", "Baseball Swing", ACTION, "stick", 0, (p, t) -> {
            float ph = saw(t, 2.0f);
            float yaw = seq(ph, 0f, 0.6f, 0.9f, 1.3f, 1.15f, -1.4f, 1.6f, -1.2f, 2f, 0.6f);
            p.rightArm(-1.6f, yaw, 0); p.leftArm(-1.6f, yaw, 0); p.bodyY = -yaw * 0.4f; p.legs(0, 0, 0.25f);
            p.lean = 0.1f; p.rootY = 0.8f;
        });
        Emotes.add("tennis_serve", "Tennis Serve", ACTION, "slime_ball", 0, (p, t) -> {
            float ph = saw(t, 2.2f);
            p.lax = seq(ph, 0f, 0f, 0.6f, -3.0f, 1.2f, -3.0f, 1.8f, 0f, 2.2f, 0f);
            p.rax = seq(ph, 0f, 0f, 0.6f, 0.8f, 1.1f, 1.0f, 1.3f, -3.0f, 1.8f, -0.6f, 2.2f, 0f);
            p.lean = -0.2f * seq(ph, 0f, 0f, 1.0f, 1f, 1.3f, 0f, 2.2f, 0f); p.rootY = -seq(ph, 0f, 0f, 1.1f, 0f, 1.3f, 2f, 1.6f, 0f);
        });
        Emotes.add("swim_freestyle", "Freestyle Swim", ACTION, "heart_of_the_sea", 0, (p, t) -> {
            float a = (t * 4f) % TAU;
            p.flip = HP; p.rootY = -4; p.rax = -a; p.lax = -a + PI;
            p.rlx = sin(t * 14) * 0.3f; p.llx = -sin(t * 14) * 0.3f; p.headY = sin(t * 2) * 0.6f; p.headX = -1.2f;
        });
        Emotes.add("backstroke", "Backstroke", ACTION, "prismarine_crystals", 0, (p, t) -> {
            float a = (t * 4f) % TAU;
            p.flip = -HP; p.rootY = -2; p.rax = a; p.lax = (a + PI) % TAU;
            p.rlx = sin(t * 14) * 0.3f; p.llx = -sin(t * 14) * 0.3f;
        });
        Emotes.add("rowing", "Rowing", ACTION, "oak_boat", 0, (p, t) -> {
            float s = sin(t * 2.5f);
            p.rootY = 10; p.legs(-1.5708f, 0, 0.1f); p.arms(-1.3f + (1 + s) * 0.35f, 0, 0.1f); p.lean = -0.3f * s;
        });
        Emotes.add("tree_pose", "Tree Pose", ACTION, "oak_sapling", 0, (p, t) -> {
            p.arms(-3.0f, 0, 0.3f); p.leftLeg(-0.6f, 0, -0.9f); p.rootX = sin(t * 1.5f) * 0.3f; p.head(-0.1f, 0, 0);
        });
        Emotes.add("warrior_pose", "Warrior Pose", ACTION, "iron_ingot", 0, (p, t) -> {
            p.rootY = 2.8f; p.legs(0, 0, 0.7f); p.arms(0, 0, HP); p.headY = -0.5f;
        });
        Emotes.add("tai_chi", "Tai Chi", ACTION, "bamboo", 0, (p, t) -> {
            float s = sin(t * 0.9f);
            p.arms(-1.3f + s * 0.4f, 0, 0.2f); p.rootX = s * 2f; p.rootY = 1.5f;
            p.rightLeg(-0.3f * pos(s), 0, 0.1f); p.leftLeg(-0.3f * pos(-s), 0, -0.1f); p.bodyY = s * 0.3f;
        });
        Emotes.add("bowling", "Bowling", ACTION, "cauldron", 0, (p, t) -> {
            float ph = saw(t, 2.4f);
            p.rax = seq(ph, 0f, 0.3f, 0.8f, 1.0f, 1.3f, -1.0f, 1.8f, -1.0f, 2.4f, 0.3f);
            p.lean = 0.5f * seq(ph, 0f, 0f, 0.8f, 0.2f, 1.3f, 1f, 1.8f, 1f, 2.4f, 0f);
            p.rlx = -0.5f * seq(ph, 0f, 0f, 1.0f, 0f, 1.3f, 1f, 2.4f, 0f); p.llx = 0.4f * (-p.rlx / 0.5f);
            p.rootY = 2f * (-p.rlx / 0.5f);
        });
        // ---------------- music
        Emotes.add("violin", "Violin", ACTION, "string", 0, (p, t) -> {
            p.rightArm(-1.4f + sin(t * 4) * 0.1f, -0.2f + sin(t * 4) * 0.35f, 0); p.leftArm(-2.0f, 0.6f, 0);
            p.headZ = 0.3f; p.rootX = sin(t * 2) * 0.5f;
        });
        Emotes.add("trumpet", "Trumpet", ACTION, "goat_horn", 0, (p, t) -> {
            p.arms(-2.3f, -0.4f, 0); p.lean = -0.15f; p.headX = -0.3f; p.rootY = -abs(sin(t * 4)) * 0.6f;
        });
        Emotes.add("piano", "Piano", ACTION, "note_block", 0, (p, t) -> {
            p.rootY = 10; p.legs(-1.5708f, 0, 0.1f);
            p.rightArm(-1.45f + sin(t * 8) * 0.15f, sin(t * 3) * 0.2f, 0); p.leftArm(-1.45f + sin(t * 8 + 2) * 0.15f, sin(t * 3 + 2) * 0.2f, 0);
            p.headX = 0.15f + sin(t * 2) * 0.1f; p.lean = 0.1f;
        });
        Emotes.add("dj", "DJ Scratch", ACTION, "jukebox", 0, (p, t) -> {
            p.rightArm(-1.0f, sin(t * 10) * 0.3f - 0.2f, 0); p.leftArm(-2.4f, 0.5f, 0);
            p.headZ = 0.25f; p.rootY = (1 - cos(t * 8)) * 0.35f; p.legs(0, 0, 0.12f);
        });
        // ---------------- everyday
        Emotes.add("tie_shoes", "Tie Your Shoes", MOOD, "string", 3.2f, (p, t) -> {
            float e = env(t, 0.8f, 1.5f, 0.9f);
            p.lean = 1.35f * e; p.arms(0.2f * e + sin(t * 9) * 0.1f * e, 0, 0); p.headX = -0.2f * e;
        });
        Emotes.add("tip_hat", "Tip the Hat", GREET, "leather_helmet", 1.8f, (p, t) -> {
            float e = env(t, 0.3f, 0.7f, 0.4f);
            p.rightArm(-2.6f * e, -0.35f * e, 0); p.headX = 0.2f * e;
        });
        Emotes.add("wipe_sweat", "Wipe Sweat", MOOD, "water_bucket", 0, (p, t) -> {
            p.rightArm(-2.5f, -0.3f + sin(t * 4) * 0.25f, 0); p.headX = -0.2f; p.lean = 0.1f;
        });
        Emotes.add("shush", "Shhh", MOOD, "string", 0, (p, t) -> {
            p.rightArm(-2.3f, -0.1f, 0); p.leftArm(0.05f, 0, -0.1f); p.headZ = 0.12f; p.headX = 0.05f;
        });
        Emotes.add("mic_drop", "Mic Drop", MOOD, "note_block", 2.6f, (p, t) -> {
            p.rax = seq(t, 0f, 0f, 0.4f, -1.5f, 1.0f, -1.5f, 1.25f, 0.1f, 2.6f, 0.1f);
            p.headX = -0.1f * seq(t, 0f, 0f, 1f, 1f, 2.6f, 1f); p.lean = -0.1f * smooth((t - 1.2f) / 0.4f); p.leftArm(0.1f, 0, -0.5f);
        });
        Emotes.add("peekaboo", "Peekaboo", SILLY, "feather", 0, (p, t) -> {
            float k = seq(saw(t, 2.6f), 0f, 1f, 1.2f, 1f, 1.5f, 0f, 2.3f, 0f, 2.6f, 1f);
            p.arms(-2.3f, -0.35f - (1 - k) * 0.8f, 0); p.headX = 0.1f * k;
        });
        Emotes.add("hug", "Hug", GREET, "pink_dye", 0, (p, t) -> {
            float s = (sin(t * 1.5f) + 1) / 2;
            p.arms(-1.5f, lerp(0.7f, -0.35f, smooth(s * 2f)), 0); p.lean = 0.1f;
        });
        Emotes.add("cheers", "Cheers!", GREET, "glass_bottle", 0, (p, t) -> {
            p.rax = -2.0f - pos(sin(t * 3)) * 0.2f; p.ray = -0.1f; p.lax = 0.05f; p.headX = -0.1f;
        });
        Emotes.add("fist_bump", "Fist Bump", GREET, "iron_nugget", 0, (p, t) -> {
            p.rax = -1.45f + pos(sin(t * 5)) * 0.15f; p.ray = 0.05f; p.lean = 0.05f;
        });
        Emotes.add("knock_knock", "Knock Knock", SILLY, "oak_door", 0, (p, t) -> {
            p.rax = -1.5f; p.ray = pos(sin(t * 10)) * 0.12f; p.lean = 0.05f; p.headX = pos(sin(t * 10)) * 0.05f;
        });
        Emotes.add("scheming", "Evil Plan", MOOD, "spider_eye", 0, (p, t) -> {
            p.arms(-1.2f, -0.45f + sin(t * 14) * 0.1f, 0); p.lean = 0.2f; p.headX = 0.3f;
        });
        Emotes.add("thumbs_down", "Thumbs Down", MOOD, "poisonous_potato", 0, (p, t) -> {
            p.rightArm(-1.45f, 0.3f, 0); p.headX = 0.2f; p.headY = sin(t * 1.5f) * 0.15f;
        });
        Emotes.add("rps", "Rock Paper Scissors", SILLY, "shears", 0, (p, t) -> {
            float ph = saw(t, 3f);
            p.rax = -1.4f - pos(sin(clamp01(ph / 2f) * PI * 3f)) * 0.5f * (ph < 2f ? 1f : 0f); p.ray = 0.1f; p.headX = 0.05f;
        });
        Emotes.add("bye_bye", "Bye Bye!", GREET, "poppy", 0, (p, t) -> {
            p.rax = -1.9f; p.ray = sin(t * 7) * 0.5f; p.headZ = 0.1f;
        });
    }
}
