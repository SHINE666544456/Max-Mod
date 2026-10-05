package com.example.emotewheel;

/** Persistent per-player toggles (not emotes): shadow clones and the cat-girl transformation. */
public final class States {
    private States() {}

    public static final String SHADOW_CLONES = "shadow_clones";
    public static final String CAT_GIRL = "cat_girl";

    /** The one account allowed to make shadow clones. */
    public static final String OWNER = "SSK1P";
    /** Set to true to make the Sexy Jutsu (cat-girl skin) owner-only too. */
    public static final boolean CAT_GIRL_OWNER_ONLY = false;

    public static boolean isKnown(String state) {
        return SHADOW_CLONES.equals(state) || CAT_GIRL.equals(state);
    }

    /** May the player with this account name switch this state on? */
    public static boolean allowed(String state, String playerName) {
        if (SHADOW_CLONES.equals(state)) return OWNER.equalsIgnoreCase(playerName);
        if (CAT_GIRL.equals(state)) return !CAT_GIRL_OWNER_ONLY || OWNER.equalsIgnoreCase(playerName);
        return false;
    }
}
