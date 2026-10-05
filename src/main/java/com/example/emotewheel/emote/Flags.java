package com.example.emotewheel.emote;

import java.util.Set;

/** Marks which emotes work while walking, and which show cat ears/tail. */
final class Flags {
    private Flags() {}

    /** Upper-body emotes: you can keep walking normally while they play. */
    private static final Set<String> WALK = Set.of(
        "wave", "big_wave", "hello", "salute", "thumbs_up", "high_five", "handshake", "beckon", "greet_both", "point_you",
        "hands_on_hips", "arms_crossed", "thinker", "shrug", "pray", "point_forward", "point_up", "hands_up", "flex",
        "peace_sign", "clap", "slow_clap", "ovation", "fist_pump", "eat", "drink", "phone_call", "selfie", "karaoke",
        "nod", "shake_head", "confused", "worried", "look_around", "laugh", "cry", "proud", "shy", "bored", "shocked",
        "finger_snap", "conductor", "raise_roof", "stir_pot", "arm_ripple", "slow_sway", "sprinkler", "windmill",
        "robot", "chicken_dance", "serve_tea", "dust_shelf", "wipe_window", "nya", "paw_wave", "heart_hands", "ear_wiggle",
        "sparkle_pose", "maid_salute", "moe_moe_kyun", "peekaboo", "cheers", "fist_bump", "knock_knock", "shush",
        "violin", "trumpet", "dj", "kakashi_reading", "kakashi_salute", "byakugan", "brooding_glare", "believe_it",
        "hand_seals", "cross_seal", "bye_bye", "hug", "thumbs_down", "scheming", "wipe_sweat", "mic_drop", "evil_genius",
        "heart_salute", "cat_ears_pose", "see_you_master", "tea_pour", "maid_polish", "cat_scratch", "cat_groom",
        "air_guitar", "air_drums", "disco", "night_fever", "floss", "dab", "salsa", "hula");

    /** Full-body emotes that keep playing while you move. */
    private static final Set<String> MOVE = Set.of(
        "naruto_run", "gojo_float", "gojo_domain", "lapse_blue", "reversal_red", "zombie", "moonwalk", "running_man",
        "run", "sprint", "march", "sneak", "runway_walk", "penguin", "duck_walk", "crab_walk", "ghost", "superman",
        "neko_run", "cat_stalk", "sage_mode", "leaf_hurricane");

    static void apply() {
        for (Emote e : Emotes.all()) {
            if (WALK.contains(e.id())) e.flag(Emote.WALK);
            if (MOVE.contains(e.id())) e.flag(Emote.MOVE);
            if (e.category() == Category.MAID || e.id().contains("cat_") || e.id().startsWith("neko")) e.flag(Emote.CAT);
        }
    }
}
