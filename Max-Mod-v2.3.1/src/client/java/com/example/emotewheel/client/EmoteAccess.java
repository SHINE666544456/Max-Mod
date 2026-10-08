package com.example.emotewheel.client;

import java.util.ArrayList;
import java.util.List;

import com.example.emotewheel.emote.Emote;
import com.example.emotewheel.emote.Emotes;

import net.minecraft.client.Minecraft;

/** Some emotes (shadow clone) only exist for one account. Everything in the UI goes through here. */
public final class EmoteAccess {
    private EmoteAccess() {}

    public static String playerName() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) return mc.player.getName().getString();
        return mc.getUser().getName();
    }

    public static boolean canUse(Emote e) {
        if (e == null) return false;
        String only = e.only();
        return only == null || only.equalsIgnoreCase(playerName());
    }

    public static List<Emote> visible() {
        List<Emote> out = new ArrayList<>();
        for (Emote e : Emotes.all()) if (canUse(e)) out.add(e);
        return out;
    }
}
