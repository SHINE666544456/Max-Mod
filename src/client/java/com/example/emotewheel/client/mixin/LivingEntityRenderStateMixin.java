package com.example.emotewheel.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.example.emotewheel.client.EmoteStateHolder;
import com.example.emotewheel.emote.Emote;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Gives every living-entity render state a few extra fields: the emote being played, for how long, and whether to show cat parts. */
@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMixin implements EmoteStateHolder {
    @Unique private Emote emotes$emote;
    @Unique private float emotes$time;
    @Unique private boolean emotes$ears, emotes$tail;

    @Override public void emotes$set(Emote emote, float time) { this.emotes$emote = emote; this.emotes$time = time; }
    @Override public void emotes$setCat(boolean ears, boolean tail) { this.emotes$ears = ears; this.emotes$tail = tail; }
    @Override public Emote emotes$getEmote() { return this.emotes$emote; }
    @Override public float emotes$getTime() { return this.emotes$time; }
    @Override public boolean emotes$catEars() { return this.emotes$ears; }
    @Override public boolean emotes$catTail() { return this.emotes$tail; }
}
