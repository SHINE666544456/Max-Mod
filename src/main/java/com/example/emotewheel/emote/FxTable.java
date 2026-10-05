package com.example.emotewheel.emote;

/** Picks a particle theme for every emote that didn't set one explicitly. */
final class FxTable {
    private FxTable() {}

    static void apply() {
        for (Emote e : Emotes.all()) {
            if (e.fx() == Fx.NONE) e.fx(infer(e));
        }
    }

    private static Fx infer(Emote e) {
        String id = e.id();
        if (has(id, "popper", "confetti", "sparkler", "fireworks", "cheers", "cheerleader",
                "hype", "victory", "party", "ovation", "fist_pump", "high_five", "fist_bump",
                "magical", "sparkle", "ymca", "disco", "night_fever", "raise_roof", "dab",
                "floss", "bounce", "pogo", "moe_moe")) return Fx.POPPER;
        if (has(id, "heart", "kiss", "love", "shy", "hug", "peace_sign", "bye_bye")) return Fx.HEARTS;
        if (has(id, "karaoke", "air_guitar", "air_drums", "dj", "conductor", "finger_snap",
                "note", "piano", "violin", "trumpet", "salsa", "macarena", "charleston")) return Fx.NOTES;
        if (has(id, "fireball", "amaterasu", "hinokami", "flame", "explosion", "kamehameha",
                "power_up", "eight_gates", "reversal_red", "detroit", "bakugo", "megumin")) return Fx.FLAME;
        if (has(id, "chidori", "raijin", "kirin", "thunder", "lightning", "cowling", "shocked",
                "electric", "raikiri")) return Fx.LIGHTNING;
        if (has(id, "rasengan", "rasen", "wind", "gust", "almighty", "leaf_hurricane", "gojo_domain",
                "hollow", "titan_roar")) return Fx.WIND;
        if (has(id, "lapse_blue", "water", "ice", "getsuga", "spirit_gun", "shiver")) return Fx.BLUE;
        if (has(id, "purple", "zoltraak", "shrine", "karma", "witch", "hollow_purple")) return Fx.PURPLE;
        if (has(id, "ghost", "soul", "spooky", "brooding", "sleeping", "sleepy")) return Fx.SOUL;
        if (has(id, "cry", "heartbroken", "rain", "disappointed")) return Fx.RAIN;
        if (has(id, "clone", "summon", "smoke", "sexy", "substitution", "gear_second")) return Fx.SMOKE;
        if (has(id, "cherry", "blossom", "curtsy", "tea", "neko", "cat_", "nya", "paw",
                "maid", "apron")) return Fx.CHERRY;
        if (has(id, "angry", "stomp", "evil", "scheming")) return Fx.ANGRY;
        if (has(id, "sage", "byakugan", "sharingan", "gojo_float", "bankai", "susanoo",
                "meditate", "pray", "one_hundred", "aura", "reinhard", "mahoraga", "infinity")) return Fx.AURA;
        if (has(id, "punch", "smash", "ora", "muda", "black_flash", "gatling", "serious",
                "plus_ultra", "shadow_box", "karate", "kick")) return Fx.POWER;
        if (e.category() == Category.DANCE) return Fx.NOTES;
        if (e.category() == Category.MAID) return Fx.SPARKLE;
        if (e.category() == Category.GREET) return Fx.SPARKLE;
        return Fx.NONE;
    }

    private static boolean has(String id, String... keys) {
        for (String k : keys) if (id.contains(k)) return true;
        return false;
    }
}
