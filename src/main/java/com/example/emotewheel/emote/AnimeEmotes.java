package com.example.emotewheel.emote;

import static com.example.emotewheel.emote.Anim.*;
import static com.example.emotewheel.emote.Category.ANIME;

import com.example.emotewheel.States;

final class AnimeEmotes {
    private AnimeEmotes() {}

    // {rax, ray, raz, lax, lay, laz, headX, headY, headZ, rootX, bodyY}
    private static final float[][] SEALS = {
        {-1.35f, -0.55f, 0,  -1.45f, 0.55f, 0,   0.1f, 0, 0,  0, 0},
        {-1.55f, -0.62f, 0,  -1.30f, 0.62f, 0,   0.1f, 0, 0,  0, 0},
        {-1.25f, -0.48f, 0,  -1.60f, 0.48f, 0,   0.15f, 0, 0, 0, 0},
        {-1.45f, -0.70f, 0,  -1.45f, 0.70f, 0,   0.1f, 0, 0,  0, 0},
    };

    static void register() {
        // ---------------------------------------------------------------- Naruto / Boruto
        Emotes.add("naruto_run", "Ninja Run", ANIME, "leather_boots", 0, (p, t) -> {
            float s = sin(t * 17);
            p.lean = 0.75f; p.arms(1.5f, 0, 0.15f); p.headX = -0.55f;
            p.rlx = s * 1.2f; p.llx = -s * 1.2f; p.rootY = -abs(s) * 0.8f;
        });
        Emotes.add("hand_seals", "Hand Seals", ANIME, "paper", 0, (p, t) -> {
            frames(p, t, 0.35f, SEALS);
            p.legs(0, 0, 0.08f);
        });
        Emotes.add("cross_seal", "Cross Seal", ANIME, "ender_eye", 0, (p, t) -> {
            p.rightArm(-1.45f, -0.75f, 0); p.leftArm(-1.6f, 0.75f, 0); p.headX = 0.1f; p.legs(0, 0, 0.1f);
            p.rootY = sin(t * 2) * 0.2f;
        });
        Emotes.add("rasengan", "Rasengan", ANIME, "snowball", 0, (p, t) -> {
            p.rightArm(-1.45f, 0.1f, 0); p.leftArm(-1.35f, 0.25f, 0);
            p.lean = 0.2f; p.rightLeg(-0.4f, 0, 0.05f); p.leftLeg(0.4f, 0, -0.05f); p.rootY = 1.5f;
            p.rootX = sin(t * 40) * 0.3f; p.bodyY = 0.3f;
        });
        Emotes.add("chidori", "Chidori", ANIME, "lightning_rod", 0, (p, t) -> {
            p.rightArm(-0.6f + sin(t * 45) * 0.04f, 0, 0.1f); p.leftArm(-1.0f, 0.45f, 0);
            p.lean = 0.4f; p.headX = -0.3f; p.legs(0, 0, 0.28f); p.rootY = 1.0f; p.rootX = sin(t * 50) * 0.25f;
        });
        Emotes.add("fireball_jutsu", "Fireball Jutsu", ANIME, "fire_charge", 4.4f, (p, t) -> {
            float seal = 1f - smooth((t - 1.5f) / 0.3f);
            float ray = -0.55f + sin(t * 14) * 0.15f;
            p.rax = seq(t, 0f, -1.35f, 1.6f, -1.35f, 2.2f, -1.5f, 3.7f, -1.5f, 4.4f, 0f);
            p.lax = p.rax;
            p.ray = ray * seal - 0.08f * (1 - seal); p.lay = -p.ray;
            p.lean = seq(t, 0f, 0f, 1.6f, 0f, 2.0f, -0.35f, 2.25f, 0.6f, 3.7f, 0.6f, 4.4f, 0f);
            p.headX = seq(t, 0f, 0f, 1.6f, 0f, 2.0f, -0.5f, 2.25f, -0.1f, 3.7f, -0.1f, 4.4f, 0f);
            p.legs(0, 0, 0.12f);
        });
        Emotes.add("sage_mode", "Sage Mode", ANIME, "glow_berries", 0, (p, t) -> {
            p.rootY = 8 + sin(t * 1.5f) * 0.6f; p.rightLeg(-1.45f, -0.85f, 0); p.leftLeg(-1.7f, 0.85f, 0);
            p.arms(-0.85f, 0.35f, 0.1f); p.head(0.1f, 0, 0);
        });
        Emotes.add("gentle_fist", "Gentle Fist", ANIME, "white_dye", 0, (p, t) -> {
            p.rightLeg(-0.35f, 0, 0.05f); p.leftLeg(0.35f, 0, -0.05f); p.rootY = 0.8f;
            p.rightArm(-1.45f + sin(t * 3) * 0.03f, 0.25f, 0); p.leftArm(-0.2f, 0, -1.1f); p.bodyY = 0.35f;
            p.head(0.05f, -0.3f, 0);
        });
        Emotes.add("byakugan", "Byakugan Stance", ANIME, "ender_eye", 0, (p, t) -> {
            p.arms(-1.2f, -0.5f, 0); p.headX = -0.05f; p.rootY = sin(t * 3) * 0.2f; p.legs(0, 0, 0.1f);
        });
        Emotes.add("kakashi_reading", "Reading (Kakashi)", ANIME, "writable_book", 0, (p, t) -> {
            p.rightArm(-1.35f, -0.35f, 0); p.leftArm(0.05f, 0, -0.12f); p.headX = 0.3f; p.lean = 0.1f;
            p.bodyY = sin(t * 1.2f) * 0.05f;
        });
        Emotes.add("kakashi_salute", "Yo!", ANIME, "iron_nugget", 0, (p, t) -> {
            p.rightArm(-2.3f + sin(t * 3) * 0.05f, 0.45f, 0); p.leftArm(0.05f, 0, -0.1f); p.headZ = 0.1f;
        });
        Emotes.add("nice_guy", "Nice Guy Pose", ANIME, "golden_carrot", 0, (p, t) -> {
            p.rightArm(-1.1f, 0.7f, 0); p.leftArm(0.1f, 0, -0.8f); p.legs(0, 0, 0.2f); p.headZ = 0.15f;
            p.rootY = -abs(sin(t * 3)) * 0.8f;
        });
        Emotes.add("leaf_hurricane", "Leaf Hurricane", ANIME, "wind_charge", 0, (p, t) -> {
            p.spin = t * 9f; p.rightLeg(-1.45f, 0, 0); p.arms(0, 0, 0.9f); p.lean = -0.1f;
        });
        Emotes.add("eight_gates", "Eight Gates", ANIME, "blaze_powder", 0, (p, t) -> {
            p.rootX = sin(t * 45) * 0.5f; p.rootY = sin(t * 37) * 0.3f; p.arms(0.1f, 0, 0.35f);
            p.legs(0, 0, 0.28f); p.lean = 0.2f; p.headX = 0.15f;
        });
        Emotes.add("believe_it", "Believe It!", ANIME, "orange_dye", 0, (p, t) -> {
            p.rightArm(-0.85f, -0.75f, 0); p.leftArm(0.1f, 0, -0.7f); p.headX = -0.15f;
            p.rootY = -pos(sin(t * 2.2f)) * 0.8f;
        });
        Emotes.add("forgive_me", "Forgive Me, Sasuke", ANIME, "redstone", 3.4f, (p, t) -> {
            p.rax = seq(t, 0f, 0f, 0.6f, -2.3f, 1.4f, -2.3f, 1.6f, -1.7f, 1.8f, -2.3f, 3.0f, -2.3f, 3.4f, 0f);
            p.ray = -0.3f * clamp01(-p.rax / 2.3f);
            p.headX = 0.2f * seq(t, 0f, 0f, 1.5f, 0f, 1.7f, 1f, 3.0f, 1f, 3.4f, 0f);
        });
        Emotes.add("troublesome", "How Troublesome", ANIME, "cobweb", 0, (p, t) -> {
            p.lean = 0.2f; p.arms(0.1f, 0, 0.05f); p.head(0.2f + sin(t * 0.6f) * 0.2f, 0, 0.1f);
            p.rootY = sin(t * 1.2f) * 0.25f;
        });
        Emotes.add("kunai_throw", "Kunai Throw", ANIME, "iron_sword", 0, (p, t) -> {
            float ph = saw(t, 1.2f);
            p.rax = seq(ph, 0f, -2.6f, 0.3f, -2.9f, 0.5f, -1.2f, 1.2f, -2.6f);
            p.lean = 0.3f * smooth((p.rax + 2.9f) / 1.7f); p.leftArm(0.2f, 0, -0.5f); p.legs(0, 0, 0.2f);
        });
        Emotes.add("summoning", "Summoning Jutsu", ANIME, "bone_meal", 2.6f, (p, t) -> {
            p.lean = seq(t, 0f, 0f, 0.6f, 1.0f, 2.0f, 1.0f, 2.6f, 0f);
            p.rightArm(0.1f, 0, 0.1f); p.leftArm(0.7f, 0, -0.4f); p.headX = -0.6f * smooth(p.lean);
            p.rootY = 1.5f * smooth(p.lean);
        });
        Emotes.add("sage_intro", "Toad Sage Intro", ANIME, "lily_pad", 0, (p, t) -> {
            p.rootY = 3; p.legs(-0.5f, 0, 0.45f); p.rightArm(-2.4f, 0.9f, 0); p.leftArm(0.3f, 0, -0.6f);
            p.head(-0.1f, 0.3f, 0.2f); p.bodyY = -0.3f;
        });
        Emotes.add("cherry_blossom", "Cherry Blossom Impact", ANIME, "cherry_sapling", 2.6f, (p, t) -> {
            p.rax = seq(t, 0f, 0f, 1.0f, 1.5f, 1.15f, -0.2f, 1.9f, -0.2f, 2.6f, 0f);
            p.lean = seq(t, 0f, 0f, 1.0f, -0.4f, 1.15f, 1.0f, 1.9f, 1.0f, 2.6f, 0f);
            p.leftArm(0.6f, 0, -0.4f); p.headX = -0.6f * smooth(p.lean); p.legs(0, 0, 0.2f);
        });
        Emotes.add("karma_pain", "Karma Pain", ANIME, "wither_rose", 0, (p, t) -> {
            p.leftArm(-1.2f, 0.9f, 0); p.rightArm(0.2f, 0, 0.3f); p.lean = 0.35f; p.head(0.4f, 0, 0.1f);
            p.rootX = sin(t * 30) * 0.4f; p.rootY = 2 + sin(t * 8) * 0.4f; p.legs(-0.3f, 0, 0.2f);
        });

        // ---------------------------------------------------------------- Jujutsu Kaisen
        Emotes.add("gojo_float", "The Honored One", ANIME, "ender_eye", 0, (p, t) -> {
            p.rootY = -6 + sin(t * 1.4f) * 1.0f; p.rightArm(-3.05f, 0, -0.1f); p.leftArm(0.05f, 0, -0.08f);
            p.head(-0.25f, 0, 0.05f); p.rlx = 0.08f; p.llx = -0.05f; p.spin = sin(t * 0.5f) * 0.35f;
        });
        Emotes.add("gojo_domain", "Domain Expansion", ANIME, "end_crystal", 0, (p, t) -> {
            p.rightArm(-1.45f, -0.5f, 0); p.leftArm(-1.6f, 0.5f, 0); p.rootY = -3 + sin(t * 2) * 0.6f;
            p.head(-0.1f, 0, 0); p.lean = -0.05f;
        });
        Emotes.add("hollow_purple", "Hollow Purple", ANIME, "amethyst_cluster", 0, (p, t) -> {
            float ph = saw(t, 4.5f);
            float spread = seq(ph, 0f, 1f, 1.5f, 1f, 2.5f, 0f, 4.0f, 0f, 4.5f, 1f);
            float ray = 0.9f * spread - 0.2f * (1 - spread);
            p.rightArm(-1.45f, ray, 0); p.leftArm(-1.45f, -ray, 0);
            p.lean = 0.3f * seq(ph, 0f, 0f, 2.5f, 0f, 3.3f, 1f, 3.9f, 0f, 4.5f, 0f); p.legs(0, 0, 0.2f);
            p.rootX = sin(t * 40) * 0.2f * (1 - spread);
        });
        Emotes.add("lapse_blue", "Lapse: Blue", ANIME, "blue_dye", 0, (p, t) -> {
            p.rightArm(-1.5f + sin(t * 35) * 0.03f, 0.1f, 0); p.leftArm(0.05f, 0, -0.1f); p.head(0, 0.15f, 0);
            p.legs(0, 0, 0.15f); p.rootY = -2 + sin(t * 2) * 0.4f;
        });
        Emotes.add("reversal_red", "Reversal: Red", ANIME, "red_dye", 0, (p, t) -> {
            p.leftArm(-1.5f + sin(t * 35) * 0.03f, -0.1f, 0); p.rightArm(0.05f, 0, 0.1f); p.head(0, -0.15f, 0);
            p.legs(0, 0, 0.15f); p.rootY = -2 + sin(t * 2) * 0.4f;
        });

        // ---------------------------------------------------------------- everything else
        Emotes.add("kamehameha", "Kamehameha", ANIME, "beacon", 0, (p, t) -> {
            float ph = saw(t, 5f);
            float charge = seq(ph, 0f, 0f, 2.5f, 1f, 3.3f, 1f, 3.4f, 0f, 5f, 0f);
            float thrust = seq(ph, 0f, 0f, 3.2f, 0f, 3.5f, 1f, 4.6f, 1f, 5f, 0f);
            p.arms(lerp(lerp(0f, 0.55f, charge), -1.57f, thrust), lerp(lerp(0f, -0.45f, charge), -0.12f, thrust), 0);
            p.lean = lerp(0.15f * charge, 0.45f, thrust); p.legs(0, 0, 0.3f); p.rootY = 0.6f;
            p.rootX = sin(t * 40) * 0.35f * charge * (1 - thrust);
        });
        Emotes.add("power_up", "Power Up", ANIME, "blaze_rod", 0, (p, t) -> {
            p.rootX = sin(t * 50) * 0.5f; p.rootY = 0.5f + sin(t * 9) * 0.3f; p.arms(0.15f, 0, 0.45f);
            p.legs(0, 0, 0.35f); p.lean = 0.15f; p.headX = -0.1f;
        });
        Emotes.add("bizarre_pose", "Bizarre Pose", ANIME, "amethyst_shard", 0, (p, t) -> {
            p.body(0, 0.5f, 0); p.lean = -0.15f; p.rightArm(-2.4f, 0.5f, -0.2f); p.leftArm(-1.7f, -0.9f, 0);
            p.head(-0.2f, -0.4f, 0.35f); p.rightLeg(-0.6f, 0.3f, 0.2f); p.leftLeg(0.3f, -0.2f, -0.1f);
            p.rootY = 0.6f; p.rootX = sin(t * 30) * 0.12f;
        });
        Emotes.add("evil_genius", "I Am Justice", ANIME, "writable_book", 0, (p, t) -> {
            p.rightArm(-2.45f, -0.35f, 0); p.head(-0.35f, 0, 0.1f); p.lean = -0.25f; p.rootX = sin(t * 20) * 0.12f;
        });
        Emotes.add("detective_crouch", "Detective Crouch", ANIME, "sweet_berries", 0, (p, t) -> {
            p.rootY = 10; p.legs(-1.5708f, 0, 0.1f); p.rightArm(-2.35f, -0.3f, 0); p.leftArm(-1.0f, 0.2f, 0);
            p.lean = 0.35f; p.head(0.2f, 0, 0);
        });
        Emotes.add("heart_salute", "Heart Salute", ANIME, "iron_chestplate", 0, (p, t) -> {
            p.rightArm(-1.0f, -1.15f, 0); p.leftArm(0.55f, 0, -0.35f); p.head(-0.2f, 0, 0); p.lean = -0.05f;
            p.legs(0, 0, 0.06f);
        });
        Emotes.add("magical_girl", "Magical Transformation", ANIME, "nether_star", 3.8f, (p, t) -> {
            float e = env(t, 0.5f, 2.6f, 0.7f);
            p.spin = TAU * 2 * smooth(t / 3.2f);
            p.rootY = -seq(t, 0f, 0f, 1.5f, 6f, 3.1f, 6f, 3.8f, 0f);
            p.arms(lerp(0f, -2.7f, e), 0, lerp(0f, -0.6f, e));
        });
        Emotes.add("gum_gum_pistol", "Gum-Gum Pistol", ANIME, "slime_ball", 0, (p, t) -> {
            float ph = saw(t, 1.8f);
            p.rax = seq(ph, 0f, 0f, 0.8f, 1.3f, 0.95f, -1.57f, 1.5f, -1.57f, 1.8f, 0f);
            p.lean = seq(ph, 0f, 0f, 0.8f, -0.35f, 0.95f, 0.55f, 1.5f, 0.55f, 1.8f, 0f);
            p.leftArm(-0.6f, 0.4f, 0.2f); p.legs(0, 0, 0.2f);
        });
        Emotes.add("breathing_stance", "Breathing Stance", ANIME, "iron_sword", 0, (p, t) -> {
            p.arms(-2.0f, -0.3f, 0); p.lean = 0.1f; p.rlx = -0.3f; p.llx = 0.35f; p.rootY = 1;
            p.bodyY = sin(t * 1.5f) * 0.15f; p.rax += sin(t * 1.5f) * 0.1f; p.lax = p.rax;
        });
        Emotes.add("smash", "Smash!", ANIME, "green_dye", 0, (p, t) -> {
            float ph = saw(t, 2.2f);
            p.rax = seq(ph, 0f, 0f, 1.0f, 1.4f, 1.15f, -1.57f, 1.8f, -1.57f, 2.2f, 0f);
            p.lean = seq(ph, 0f, 0f, 1.0f, -0.2f, 1.15f, 0.5f, 1.8f, 0.5f, 2.2f, 0f);
            p.rootX = sin(t * 45) * 0.3f * seq(ph, 0f, 0f, 0.3f, 1f, 1.0f, 1f, 1.1f, 0f);
            p.leftArm(-0.5f, 0.3f, 0.2f); p.legs(0, 0, 0.25f);
        });
        Emotes.add("brooding_glare", "Brooding Glare", ANIME, "wither_skeleton_skull", 0, (p, t) -> {
            p.rightArm(-1.05f, -0.95f, 0); p.leftArm(-1.3f, 0.95f, 0); p.head(0.12f, 0.3f, 0.1f);
            p.body(0, 0.15f, 0); p.lean = -0.05f; p.rootX = sin(t) * 0.4f;
        });

        // ---------------------------------------------------------------- toggles (see Flags / States)
        Emotes.add("shadow_clone", "Shadow Clone Jutsu", ANIME, "ender_pearl", 1.8f, (p, t) -> {
            float e = env(t, 0.25f, 1.0f, 0.35f);
            p.rightArm(lerp(0f, -1.45f, e), lerp(0f, -0.75f, e), 0); p.leftArm(lerp(0f, -1.6f, e), lerp(0f, 0.75f, e), 0);
            p.headX = 0.1f * e; p.legs(0, 0, 0.1f * e);
        }).state(States.SHADOW_CLONES, 8).only(States.OWNER);

        Emotes.add("sexy_jutsu", "Sexy Jutsu", ANIME, "pink_petals", 2.2f, (p, t) -> {
            float seal = env(t, 0.2f, 0.4f, 0.2f);
            float pose = seq(t, 0f, 0f, 0.7f, 0f, 1.0f, 1f, 1.9f, 1f, 2.2f, 0f);
            p.rightArm(lerp(0f, -1.45f, seal) + pose * -1.1f, lerp(0f, -0.75f, seal), 0);   // seal, then peace sign
            p.leftArm(lerp(0f, -1.6f, seal) + pose * 1.7f, lerp(0f, 0.75f, seal) - pose * 0.75f, -pose * 0.8f); // hand on hip
            p.head(0.1f * seal, 0, 0.25f * pose); p.spin = TAU * smooth((t - 0.7f) / 0.5f);
            p.legs(0, 0, 0.1f * pose);
        }).state(States.CAT_GIRL, 10).only(States.CAT_GIRL_OWNER_ONLY ? States.OWNER : null);
    }
}
