package com.example.emotewheel.client;

import com.example.emotewheel.emote.Emote;

/** Implemented (via mixin) by LivingEntityRenderState so a player's current emote and cat parts travel with the render state. */
public interface EmoteStateHolder {
    void emotes$set(Emote emote, float time);
    void emotes$setCat(boolean ears, boolean tail);
    Emote emotes$getEmote();
    float emotes$getTime();
    boolean emotes$catEars();
    boolean emotes$catTail();
}
