package com.example.emotewheel;

/** Persistent per-player toggles (not emotes): shadow clones and the cat-girl transformation. */
public final class States {
    private States() {}

    /** Basic shadow clones (a few). Available to everyone. */
    public static final String SHADOW_CLONES = "shadow_clones";
    /** Exactly one clone. Available to everyone. */
    public static final String JUST_A_CLONE = "just_a_clone";
    /** Many clones; count is configurable in settings. Owner only. */
    public static final String MULTI_SHADOW_CLONES = "multi_shadow_clones";
    public static final String CAT_GIRL = "cat_girl";
    /** Turn into the block you hold in your off hand. Owner only. */
    public static final String SUBSTITUTION = "substitution";
    /** Hide your nametag and locator-bar dot. Owner only. */
    public static final String STEALTH = "stealth";

    /** The one account allowed to use owner-only jutsu. */
    public static final String OWNER = "SSK1P";
    /** Set to true to make the Sexy Jutsu (cat-girl skin) owner-only too. */
    public static final boolean CAT_GIRL_OWNER_ONLY = false;

    /** All known state ids (for network sync / validation). */
    public static final String[] ALL = {
        SHADOW_CLONES, JUST_A_CLONE, MULTI_SHADOW_CLONES, CAT_GIRL, SUBSTITUTION, STEALTH
    };

    /** Clone-related states (mutually exclusive when toggling). */
    public static final String[] CLONE_STATES = {
        SHADOW_CLONES, JUST_A_CLONE, MULTI_SHADOW_CLONES
    };

    public static boolean isKnown(String state) {
        for (String s : ALL) if (s.equals(state)) return true;
        return false;
    }

    public static boolean isCloneState(String state) {
        for (String s : CLONE_STATES) if (s.equals(state)) return true;
        return false;
    }

    /** May the player with this account name switch this state on? */
    public static boolean allowed(String state, String playerName) {
        if (SHADOW_CLONES.equals(state) || JUST_A_CLONE.equals(state)) return true;
        if (MULTI_SHADOW_CLONES.equals(state) || SUBSTITUTION.equals(state) || STEALTH.equals(state))
            return OWNER.equalsIgnoreCase(playerName);
        if (CAT_GIRL.equals(state)) return !CAT_GIRL_OWNER_ONLY || OWNER.equalsIgnoreCase(playerName);
        return false;
    }
}
