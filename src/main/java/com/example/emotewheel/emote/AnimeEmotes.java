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
            float ph = saw(t, 5.6f);
            float spread = seq(ph, 0f, 1f, 2.1f, 1f, 2.75f, 0f, 5.6f, 0f);
            float smash = seq(ph, 0f, 0f, 2.15f, 0f, 2.55f, 1f, 3.15f, 0.25f, 5.6f, 0.15f);
            p.rightArm(-1.48f, 0.95f * spread, 0);
            p.leftArm(-1.48f, -0.95f * spread, 0);
            p.headX = -0.08f * smash;
            p.lean = 0.12f + 0.5f * smash;
            p.legs(0, 0, 0.22f);
            p.rootX = sin(t * 48) * 0.28f * smash;
            p.rootY = -0.4f * smash;
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

        // extra anime references
        Emotes.add("rasenshuriken", "Rasen-Shuriken", ANIME, "wind_charge", 0, (p, t) -> {
            p.rightArm(-2.6f, 0.35f, -0.2f); p.leftArm(-2.4f, -0.2f, 0.15f);
            p.spin = t * 14f; p.rootY = -2 + sin(t * 20) * 0.4f; p.legs(0, 0, 0.2f);
            p.headX = -0.2f; p.lean = -0.1f;
        });
        Emotes.add("sharingan", "Sharingan", ANIME, "redstone", 0, (p, t) -> {
            p.rightArm(-2.1f, -0.55f, 0.1f); p.leftArm(0.05f, 0, -0.1f);
            p.head(-0.05f, 0.25f, 0); p.rootY = sin(t * 4) * 0.15f;
        });
        Emotes.add("amaterasu", "Black Flames", ANIME, "coal", 0, (p, t) -> {
            p.rightArm(-1.55f, 0.2f, 0); p.leftArm(-0.4f, 0.3f, -0.2f);
            p.head(0.1f, 0.2f, 0); p.lean = 0.15f; p.rootX = sin(t * 18) * 0.2f;
            p.legs(0, 0, 0.12f);
        });
        Emotes.add("susanoo", "Susanoo", ANIME, "netherite_chestplate", 0, (p, t) -> {
            p.arms(-0.2f, 0, 1.15f); p.lean = -0.15f; p.headX = -0.2f;
            p.rootY = -3 + sin(t * 1.6f) * 0.5f; p.legs(-0.2f, 0, 0.25f);
            p.bodyY = sin(t * 2) * 0.1f;
        });
        Emotes.add("substitution", "Substitution", ANIME, "oak_log", 1.6f, (p, t) -> {
            float e = env(t, 0.12f, 0.5f, 0.7f);
            p.rootY = 6 * e; p.arms(0.4f * e, 0, 0.2f); p.headX = 0.4f * e; p.lean = 0.5f * e;
            p.spin = TAU * smooth(t / 1.4f);
        });
        Emotes.add("flying_raijin", "Flying Thunder God", ANIME, "ender_pearl", 0, (p, t) -> {
            p.rightArm(-1.57f, -0.15f, 0); p.leftArm(-0.5f, 0.4f, 0.2f);
            p.lean = 0.45f; p.headX = -0.25f; p.rootY = -abs(sin(t * 8)) * 2.5f;
            p.rlx = -0.6f; p.llx = 0.4f;
        });
        Emotes.add("eight_trigrams", "Eight Trigrams", ANIME, "white_dye", 0, (p, t) -> {
            p.spin = t * 11f; p.arms(-1.2f + sin(t * 20) * 0.4f, 0.4f, 0.1f);
            p.legs(0, 0, 0.25f); p.rootY = 0.5f; p.headX = -0.1f;
        });
        Emotes.add("almighty_push", "Almighty Push", ANIME, "wind_charge", 3.2f, (p, t) -> {
            float e = env(t, 0.45f, 1.6f, 0.8f);
            p.arms(lerp(0.2f, -1.55f, e), 0, lerp(0.1f, 0.85f, e));
            p.lean = lerp(0.2f, -0.25f, e); p.headX = -0.2f * e;
            p.rootY = -2f * e; p.legs(0, 0, 0.2f * e);
        });
        Emotes.add("kirin", "Kirin", ANIME, "lightning_rod", 3.0f, (p, t) -> {
            float e = env(t, 0.4f, 1.8f, 0.6f);
            p.arms(lerp(0f, -2.85f, e), 0, lerp(0f, -0.35f, e));
            p.headX = -0.5f * e; p.lean = -0.2f * e; p.rootY = -4f * e;
        });
        Emotes.add("black_flash", "Black Flash", ANIME, "netherite_ingot", 0, (p, t) -> {
            float ph = saw(t, 1.5f);
            p.rax = seq(ph, 0f, 0.4f, 0.35f, 1.2f, 0.5f, -1.7f, 1.1f, -1.7f, 1.5f, 0.4f);
            p.lean = seq(ph, 0f, 0.1f, 0.35f, -0.3f, 0.5f, 0.7f, 1.1f, 0.5f, 1.5f, 0.1f);
            p.leftArm(-0.6f, 0.45f, 0.15f); p.legs(0, 0, 0.22f);
            p.rootX = sin(t * 50) * 0.25f * seq(ph, 0f, 0f, 0.4f, 1f, 0.55f, 0f);
        });
        Emotes.add("shrine_domain", "Malevolent Shrine", ANIME, "nether_wart", 0, (p, t) -> {
            p.rightArm(-1.5f, -0.7f, 0); p.leftArm(-1.5f, 0.7f, 0);
            p.rootY = -2 + sin(t * 2.2f) * 0.5f; p.headX = 0.15f; p.lean = -0.08f;
            p.spin = sin(t * 0.8f) * 0.2f;
        });
        Emotes.add("boogie_woogie", "Boogie Woogie", ANIME, "note_block", 0, (p, t) -> {
            float ph = saw(t, 0.7f);
            p.rax = seq(ph, 0f, -0.4f, 0.15f, -1.5f, 0.28f, -0.2f, 0.7f, -0.4f);
            p.lax = p.rax; p.ray = 0.35f; p.lay = -0.35f;
            p.headX = 0.1f; p.rootY = -abs(sin(t * 9)) * 0.6f;
        });
        Emotes.add("ten_shadows", "Ten Shadows", ANIME, "sculk", 0, (p, t) -> {
            frames(p, t, 0.4f, SEALS);
            p.lean = 0.15f; p.legs(0, 0, 0.12f); p.rootY = sin(t * 3) * 0.2f;
        });
        Emotes.add("ora_ora", "Ora Ora", ANIME, "gold_ingot", 0, (p, t) -> {
            float s = sin(t * 28);
            p.rax = -1.4f - pos(s) * 0.5f; p.lax = -1.4f - pos(-s) * 0.5f;
            p.ray = 0.15f; p.lay = -0.15f; p.lean = 0.35f; p.headX = -0.15f;
            p.legs(0, 0, 0.22f); p.rootY = abs(s) * 0.4f;
        });
        Emotes.add("muda_muda", "Muda Muda", ANIME, "gold_nugget", 0, (p, t) -> {
            float s = sin(t * 32);
            p.rax = -1.55f + s * 0.35f; p.lax = -1.55f - s * 0.35f;
            p.lean = 0.4f; p.headX = -0.2f; p.legs(0, 0, 0.25f);
            p.rootX = s * 0.5f;
        });
        Emotes.add("za_warudo", "Time Stop", ANIME, "clock", 0, (p, t) -> {
            p.rightArm(-1.7f, 0.15f, 0.2f); p.leftArm(-0.2f, 0, -0.85f);
            p.head(-0.15f, 0.2f, 0.15f); p.lean = -0.1f; p.legs(0, 0, 0.15f);
            p.rootY = sin(t * 1.2f) * 0.3f;
        });
        Emotes.add("star_finger", "Star Finger", ANIME, "arrow", 0, (p, t) -> {
            p.rax = -1.57f + sin(t * 6) * 0.04f; p.ray = -0.05f; p.leftArm(-0.8f, 0.5f, 0.15f);
            p.lean = 0.2f; p.headX = -0.05f; p.legs(0, 0, 0.12f);
        });
        Emotes.add("gear_second", "Gear Second", ANIME, "campfire", 0, (p, t) -> {
            float s = sin(t * 18);
            p.lean = 0.55f; p.arms(1.2f, 0, 0.2f); p.headX = -0.4f;
            p.rlx = s * 1.1f; p.llx = -s * 1.1f; p.rootY = -abs(s) * 1.2f;
        });
        Emotes.add("gum_gatling", "Gum-Gum Gatling", ANIME, "slime_ball", 0, (p, t) -> {
            float s = sin(t * 26);
            p.rax = -1.2f - pos(s) * 0.7f; p.lax = -1.2f - pos(-s) * 0.7f;
            p.lean = 0.45f; p.legs(0, 0, 0.2f); p.rootX = s * 0.4f;
        });
        Emotes.add("three_swords", "Three Swords", ANIME, "iron_sword", 0, (p, t) -> {
            p.rightArm(-0.9f, 0.7f, 0.15f); p.leftArm(-0.9f, -0.7f, -0.15f);
            p.headX = 0.35f; p.lean = 0.2f; p.rlx = -0.35f; p.llx = 0.35f;
            p.rootY = sin(t * 3) * 0.2f;
        });
        Emotes.add("water_wheel", "Water Wheel", ANIME, "water_bucket", 0, (p, t) -> {
            p.spin = t * 8f; p.flip = 0.4f; p.arms(-0.3f, 0, 0.9f);
            p.rightLeg(-1.2f, 0, 0); p.leftLeg(0.4f, 0, 0); p.lean = 0.2f;
        });
        Emotes.add("thunderclap", "Thunderclap Flash", ANIME, "lightning_rod", 0, (p, t) -> {
            p.lean = 0.7f; p.arms(-0.4f, 0.2f, 0.15f); p.headX = -0.35f;
            p.rlx = -1.3f; p.llx = 0.6f; p.rootY = -abs(sin(t * 16)) * 1.5f;
            p.rootX = sin(t * 40) * 0.2f;
        });
        Emotes.add("hinokami", "Hinokami Kagura", ANIME, "blaze_powder", 0, (p, t) -> {
            p.spin = t * 6f; p.rightArm(-2.2f, 0.4f, -0.2f); p.leftArm(-0.6f, 0.5f, 0.3f);
            p.lean = 0.15f; p.rootY = -1 + sin(t * 8) * 0.5f; p.legs(-0.3f, 0, 0.2f);
        });
        Emotes.add("plus_ultra", "Plus Ultra!", ANIME, "lime_banner", 0, (p, t) -> {
            p.arms(-2.9f, 0, -0.25f); p.headX = -0.35f; p.lean = -0.2f;
            p.rootY = -abs(sin(t * 4)) * 1.5f; p.legs(0, 0, 0.15f);
        });
        Emotes.add("detroit_smash", "Detroit Smash", ANIME, "green_dye", 0, (p, t) -> {
            float ph = saw(t, 2.4f);
            p.rax = seq(ph, 0f, 0.2f, 0.9f, 1.5f, 1.1f, -1.7f, 1.9f, -1.7f, 2.4f, 0.2f);
            p.lean = seq(ph, 0f, 0.1f, 0.9f, -0.4f, 1.1f, 0.7f, 1.9f, 0.5f, 2.4f, 0.1f);
            p.leftArm(-0.5f, 0.4f, 0.2f); p.legs(0, 0, 0.25f);
            p.rootX = sin(t * 40) * 0.3f * seq(ph, 0f, 0f, 0.9f, 1f, 1.2f, 0f);
        });
        Emotes.add("full_cowling", "Full Cowling", ANIME, "lime_dye", 0, (p, t) -> {
            p.rootX = sin(t * 40) * 0.35f; p.arms(0.1f, 0, 0.35f); p.legs(0, 0, 0.22f);
            p.lean = 0.2f; p.headX = -0.1f; p.rootY = sin(t * 8) * 0.25f;
        });
        Emotes.add("explosion_hero", "Explosion Stance", ANIME, "tnt", 0, (p, t) -> {
            p.rightArm(0.4f, 0, 0.9f); p.leftArm(0.4f, 0, -0.9f);
            p.lean = 0.25f; p.headX = -0.15f; p.legs(0, 0, 0.2f);
            p.rootX = sin(t * 22) * 0.2f;
        });
        Emotes.add("ice_and_fire", "Ice and Fire", ANIME, "packed_ice", 0, (p, t) -> {
            p.rightArm(-1.5f, 0.55f, 0); p.leftArm(-1.5f, -0.55f, 0);
            p.headY = 0.2f; p.legs(0, 0, 0.15f); p.rootY = sin(t * 2) * 0.3f;
        });
        Emotes.add("bankai", "Bankai", ANIME, "netherite_sword", 3.4f, (p, t) -> {
            float e = env(t, 0.5f, 2.2f, 0.6f);
            p.rightArm(lerp(0.1f, -2.8f, e), lerp(0f, 0.2f, e), 0);
            p.leftArm(lerp(0f, -0.4f, e), 0, lerp(0f, -0.4f, e));
            p.headX = -0.3f * e; p.lean = -0.15f * e; p.rootY = -3f * e;
            p.spin = 0.4f * e * sin(t * 6);
        });
        Emotes.add("getsuga", "Getsuga Tensho", ANIME, "blue_ice", 0, (p, t) -> {
            float ph = saw(t, 2.0f);
            p.rax = seq(ph, 0f, -0.4f, 0.7f, -2.6f, 0.9f, -0.2f, 1.5f, -0.2f, 2.0f, -0.4f);
            p.lean = seq(ph, 0f, 0.1f, 0.7f, -0.25f, 0.9f, 0.65f, 1.5f, 0.4f, 2.0f, 0.1f);
            p.leftArm(-0.5f, 0.3f, 0.2f); p.legs(0, 0, 0.2f);
        });
        Emotes.add("spirit_gun", "Spirit Gun", ANIME, "end_rod", 0, (p, t) -> {
            p.rightArm(-1.55f + sin(t * 20) * 0.04f, 0.05f, 0); p.leftArm(-1.1f, 0.45f, 0.1f);
            p.lean = 0.25f; p.headX = -0.1f; p.legs(0, 0, 0.15f);
            p.rootX = sin(t * 30) * 0.15f;
        });
        Emotes.add("serious_punch", "Serious Punch", ANIME, "yellow_dye", 0, (p, t) -> {
            float ph = saw(t, 2.6f);
            p.rax = seq(ph, 0f, 0.15f, 1.2f, 1.3f, 1.4f, -1.65f, 2.2f, -1.65f, 2.6f, 0.15f);
            p.lean = seq(ph, 0f, 0.05f, 1.2f, -0.2f, 1.4f, 0.55f, 2.2f, 0.4f, 2.6f, 0.05f);
            p.leftArm(0.2f, 0, -0.4f); p.legs(0, 0, 0.2f);
        });
        Emotes.add("death_note", "Write the Name", ANIME, "writable_book", 0, (p, t) -> {
            p.leftArm(-1.25f, 0.35f, 0); p.rightArm(-1.05f, -0.15f, 0.15f);
            p.headX = 0.35f; p.lean = 0.12f; p.rootY = sin(t * 1.4f) * 0.15f;
        });
        Emotes.add("titan_roar", "Titan Roar", ANIME, "bone", 0, (p, t) -> {
            p.arms(-2.7f, 0, -0.4f); p.headX = -0.55f; p.lean = -0.25f;
            p.rootY = sin(t * 12) * 0.5f; p.legs(0, 0, 0.2f);
        });
        Emotes.add("odm_gear", "ODM Gear", ANIME, "lead", 0, (p, t) -> {
            p.lean = 0.85f; p.arms(-0.3f, 0.5f, 0.4f); p.headX = -0.4f;
            p.rlx = -1.1f; p.llx = 0.5f; p.rootY = -4 + sin(t * 5) * 1.2f;
            p.spin = sin(t * 2) * 0.4f;
        });
        Emotes.add("explosion_spell", "Explosion!!", ANIME, "fire_charge", 3.6f, (p, t) -> {
            float e = env(t, 0.5f, 2.2f, 0.7f);
            p.rightArm(lerp(0f, -2.7f, e), lerp(0f, 0.15f, e), lerp(0f, -0.2f, e));
            p.leftArm(lerp(0f, -2.5f, e), lerp(0f, -0.2f, e), 0);
            p.headX = -0.45f * e; p.lean = -0.15f * e;
            p.rootY = -1.5f * e + sin(t * 30) * 0.3f * e;
        });
        Emotes.add("zoltraak", "Zoltraak", ANIME, "amethyst_shard", 0, (p, t) -> {
            p.rightArm(-1.7f, 0.1f, 0); p.leftArm(-1.2f, 0.35f, 0.1f);
            p.headX = -0.1f; p.lean = 0.1f; p.legs(0, 0, 0.1f);
            p.rootY = sin(t * 3) * 0.25f; p.rootX = sin(t * 25) * 0.12f;
        });
        Emotes.add("chainsaw_rev", "Chainsaw Rev", ANIME, "iron_axe", 0, (p, t) -> {
            p.rightArm(-1.2f, 0.2f, 0.1f); p.leftArm(-1.1f, -0.15f, -0.1f);
            p.lean = 0.3f; p.headX = -0.15f; p.legs(0, 0, 0.2f);
            p.rootX = sin(t * 40) * 0.35f;
        });
        Emotes.add("one_hundred", "???%", ANIME, "end_crystal", 0, (p, t) -> {
            p.rootY = -5 + sin(t * 2) * 0.8f; p.arms(-0.15f, 0, 0.2f);
            p.headX = -0.2f; p.spin = sin(t * 0.7f) * 0.5f; p.rlx = 0.1f; p.llx = -0.1f;
        });

        // ---------------------------------------------------------------- Re:Zero
        Emotes.add("aura_monster", "Aura Monster", ANIME, "wither_rose", 0, (p, t) -> {
            // Subaru meme: overwhelming witch miasma, slight lean, crazy head tilt, self-point
            p.rightArm(-1.55f + sin(t * 3) * 0.04f, 0.12f, 0.05f);
            p.leftArm(0.15f, 0, -0.85f);
            p.head(-0.18f, 0.28f, 0.18f);
            p.lean = -0.12f;
            p.bodyY = 0.12f;
            p.rootY = -1.2f + sin(t * 2.4f) * 0.45f;
            p.rootX = sin(t * 14) * 0.18f;
            p.legs(0, 0, 0.12f);
        }).fx(Fx.PURPLE);
        Emotes.add("aura_farm", "Aura Farm", ANIME, "sculk", 0, (p, t) -> {
            float s = sin(t * 6);
            p.lean = 0.18f; p.arms(0.05f, 0, 0.28f);
            p.head(-0.12f, 0.15f, 0.08f);
            p.rlx = s * 0.55f; p.llx = -s * 0.55f;
            p.rootY = -0.6f + abs(s) * 0.3f;
            p.bodyY = sin(t * 2) * 0.08f;
        }).fx(Fx.PURPLE);
        Emotes.add("return_by_death", "Return by Death", ANIME, "redstone", 3.2f, (p, t) -> {
            float e = env(t, 0.25f, 2.2f, 0.6f);
            p.rightArm(lerp(0f, -1.35f, e), lerp(0f, -0.55f, e), 0);
            p.leftArm(lerp(0f, -1.15f, e), lerp(0f, 0.45f, e), 0);
            p.headX = 0.45f * e; p.lean = 0.55f * e;
            p.rootX = sin(t * 28) * 0.45f * e; p.rootY = 2.5f * e;
            p.legs(-0.2f * e, 0, 0.1f);
        }).fx(Fx.SOUL);
        Emotes.add("i_love_emilia", "I Love Emilia", ANIME, "pink_tulip", 0, (p, t) -> {
            p.rightArm(-2.55f, -0.15f, -0.1f); p.leftArm(-0.2f, 0, -0.55f);
            p.head(-0.25f, 0.1f, 0); p.lean = -0.08f;
            p.rootY = -abs(sin(t * 3.5f)) * 0.7f; p.legs(0, 0, 0.08f);
        }).fx(Fx.HEARTS);
        Emotes.add("witch_scent", "Witch's Scent", ANIME, "purple_dye", 0, (p, t) -> {
            p.lean = 0.35f; p.headX = -0.35f; p.arms(-0.7f, 0.25f, 0.15f);
            p.rootX = sin(t * 8) * 0.25f; p.rootY = sin(t * 2) * 0.3f;
            p.legs(0, 0, 0.12f);
        }).fx(Fx.PURPLE);
        Emotes.add("barusu", "Barusu!", ANIME, "iron_axe", 0, (p, t) -> {
            p.rax = -1.57f + sin(t * 8) * 0.08f; p.ray = -0.1f;
            p.leftArm(-0.4f, 0.5f, 0.2f); p.headX = -0.1f; p.lean = 0.2f;
            p.legs(0, 0, 0.15f);
        });
        Emotes.add("betty_covenant", "Betty's Contractor", ANIME, "book", 0, (p, t) -> {
            p.arms(-1.05f, -0.55f, 0); p.head(0.08f, 0.2f, 0.12f);
            p.lean = -0.05f; p.rootY = sin(t * 1.6f) * 0.2f; p.legs(0, 0, 0.08f);
        }).fx(Fx.SPARKLE);
        Emotes.add("rem_flail", "Rem's Flail", ANIME, "iron_axe", 0, (p, t) -> {
            float ph = saw(t, 1.4f);
            p.rax = seq(ph, 0f, 0.4f, 0.35f, -2.5f, 0.55f, -0.2f, 1.0f, -0.2f, 1.4f, 0.4f);
            p.lean = seq(ph, 0f, 0.1f, 0.35f, -0.3f, 0.55f, 0.55f, 1.0f, 0.3f, 1.4f, 0.1f);
            p.leftArm(-0.5f, 0.4f, 0.2f); p.legs(0, 0, 0.2f);
            p.spin = seq(ph, 0f, 0f, 0.5f, 0.4f, 1.0f, 0f, 1.4f, 0f);
        }).fx(Fx.POWER);
        Emotes.add("reinhard_draw", "Sword Saint", ANIME, "netherite_sword", 0, (p, t) -> {
            p.rightArm(-0.35f, 0.7f, 0.15f); p.leftArm(-1.8f, -0.4f, 0);
            p.lean = 0.12f; p.headX = -0.08f; p.rlx = -0.35f; p.llx = 0.25f;
            p.rootY = sin(t * 2) * 0.2f;
        }).fx(Fx.AURA);
        Emotes.add("from_zero", "From Zero", ANIME, "soul_lantern", 3.6f, (p, t) -> {
            float down = env(t, 0.4f, 1.2f, 0.5f);
            float up = seq(t, 0f, 0f, 1.8f, 0f, 2.3f, 1f, 3.6f, 1f);
            p.rootY = 8f * down * (1f - up);
            p.lean = 0.7f * down * (1f - up) - 0.15f * up;
            p.arms(lerp(0.4f, -2.6f, up), 0, lerp(0.1f, -0.25f, up));
            p.headX = 0.4f * down * (1f - up) - 0.2f * up;
        }).fx(Fx.SOUL);

        // ---------------------------------------------------------------- more Jujutsu Kaisen
        Emotes.add("infinity", "Infinity", ANIME, "glass", 0, (p, t) -> {
            p.rightArm(-1.45f, 0.15f, 0); p.leftArm(0.05f, 0, -0.12f);
            p.head(-0.2f, 0.1f, 0.05f); p.rootY = -3 + sin(t * 1.5f) * 0.5f;
            p.lean = -0.05f; p.spin = sin(t * 0.6f) * 0.2f;
        }).fx(Fx.AURA);
        Emotes.add("six_eyes", "Six Eyes", ANIME, "ender_eye", 0, (p, t) -> {
            p.rightArm(-2.15f, -0.45f, 0.1f); p.leftArm(-2.15f, 0.45f, -0.1f);
            p.head(-0.05f, 0, 0); p.rootY = sin(t * 3) * 0.2f;
        }).fx(Fx.BLUE);
        Emotes.add("simple_domain", "Simple Domain", ANIME, "iron_bars", 0, (p, t) -> {
            p.rightArm(-1.5f, -0.55f, 0); p.leftArm(-1.5f, 0.55f, 0);
            p.legs(0, 0, 0.2f); p.rootY = sin(t * 2) * 0.25f; p.headX = 0.1f;
        }).fx(Fx.WIND);
        Emotes.add("ratio_strike", "7:3 Ratio", ANIME, "golden_sword", 0, (p, t) -> {
            float ph = saw(t, 1.6f);
            p.rax = seq(ph, 0f, -0.3f, 0.5f, -2.4f, 0.7f, -0.4f, 1.2f, -0.4f, 1.6f, -0.3f);
            p.lean = seq(ph, 0f, 0.1f, 0.5f, -0.2f, 0.7f, 0.55f, 1.2f, 0.3f, 1.6f, 0.1f);
            p.leftArm(-0.5f, 0.35f, 0.2f); p.legs(0, 0, 0.18f);
        }).fx(Fx.POWER);
        Emotes.add("cursed_speech", "Cursed Speech", ANIME, "goat_horn", 0, (p, t) -> {
            p.rightArm(-2.2f, -0.35f, 0); p.leftArm(-2.15f, 0.35f, 0);
            p.headX = -0.15f; p.lean = -0.08f;
            p.rootY = sin(t * 10) * 0.2f;
        }).fx(Fx.WIND);
        Emotes.add("piercing_blood", "Piercing Blood", ANIME, "redstone", 0, (p, t) -> {
            p.rightArm(-1.55f, 0.05f, 0); p.leftArm(-1.45f, 0.2f, 0.08f);
            p.lean = 0.28f; p.headX = -0.12f; p.legs(0, 0, 0.15f);
            p.rootX = sin(t * 22) * 0.12f;
        }).fx(Fx.FLAME);
        Emotes.add("divergent_fist", "Divergent Fist", ANIME, "raw_gold", 0, (p, t) -> {
            float ph = saw(t, 1.3f);
            p.rax = seq(ph, 0f, 0.3f, 0.4f, 1.1f, 0.55f, -1.65f, 1.05f, -1.65f, 1.3f, 0.3f);
            p.lean = seq(ph, 0f, 0.1f, 0.4f, -0.25f, 0.55f, 0.6f, 1.05f, 0.4f, 1.3f, 0.1f);
            p.leftArm(-0.5f, 0.4f, 0.15f); p.legs(0, 0, 0.2f);
        }).fx(Fx.POWER);
        Emotes.add("sukuna_laugh", "Sukuna's Laugh", ANIME, "wither_skeleton_skull", 0, (p, t) -> {
            p.lean = -0.32f; p.headX = -0.45f + sin(t * 16) * 0.08f;
            p.rightArm(-0.5f, 0, 0.95f); p.leftArm(-0.5f, 0, -0.95f);
            p.rootY = sin(t * 14) * 0.45f; p.legs(0, 0, 0.15f);
        }).fx(Fx.ANGRY);
        Emotes.add("fuga_open", "Open... Fuga", ANIME, "blaze_powder", 3.0f, (p, t) -> {
            float e = env(t, 0.4f, 1.8f, 0.6f);
            p.rightArm(lerp(0.1f, -1.7f, e), lerp(0f, 0.15f, e), 0);
            p.leftArm(lerp(0f, -0.4f, e), 0, lerp(0f, -0.3f, e));
            p.headX = -0.2f * e; p.lean = 0.15f * e;
            p.rootY = -1.5f * e; p.rootX = sin(t * 20) * 0.2f * e;
        }).fx(Fx.FLAME);
        Emotes.add("mahoraga", "Mahoraga Wheel", ANIME, "lodestone", 0, (p, t) -> {
            p.spin = t * 4f; p.arms(-0.2f, 0, 0.9f); p.headX = -0.1f;
            p.rootY = -1 + sin(t * 3) * 0.4f; p.legs(-0.15f, 0, 0.2f);
        }).fx(Fx.AURA);
        Emotes.add("hairpin", "Hairpin", ANIME, "tripwire_hook", 0, (p, t) -> {
            float ph = saw(t, 1.5f);
            p.rax = seq(ph, 0f, -0.4f, 0.5f, -2.6f, 0.7f, -1.0f, 1.2f, -1.0f, 1.5f, -0.4f);
            p.leftArm(-1.1f, 0.4f, 0.1f); p.lean = 0.15f; p.headX = 0.1f;
            p.legs(0, 0, 0.12f);
        }).fx(Fx.POWER);

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
