package com.example.emotewheel.client;

import com.example.emotewheel.emote.Emote;

import net.minecraft.world.level.block.state.BlockState;

/** Implemented (via mixin) by LivingEntityRenderState: everything this mod wants to remember about one player's render. */
public interface EmoteStateHolder {
    void emotes$set(Emote emote, float time);
    /** Ear and tail style to show on the body (0 = none). */
    void emotes$setCat(int earStyle, int tailStyle);
    /** The block this player is disguised as (or null), and which head pet to show (0 = none). */
    void emotes$setExtras(BlockState disguise, int pet);
    void emotes$setSelf(boolean self);
    Emote emotes$getEmote();
    float emotes$getTime();
    int emotes$earStyle();
    int emotes$tailStyle();
    BlockState emotes$disguise();
    int emotes$pet();
    boolean emotes$isSelf();
}
