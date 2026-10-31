package com.example.emotewheel.client;

import net.minecraft.network.chat.Component;

/** Name check for the maid outfits. */
public final class MaidUtil {
    private MaidUtil() {}

    /** True if the item's custom name is "made" or "maid" (any capitalisation). */
    public static boolean isMaidName(Component name) {
        if (name == null) return false;
        String s = name.getString().trim();
        return s.equalsIgnoreCase("made") || s.equalsIgnoreCase("maid");
    }
}
