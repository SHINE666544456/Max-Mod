package com.example.emotewheel.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.example.emotewheel.client.EmoteStateHolder;
import com.example.emotewheel.emote.Emote;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.level.block.state.BlockState;

/** Gives every living-entity render state a few extra fields: the emote being played, for how long, and whether to show cat parts. */
@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMixin implements EmoteStateHolder {
    @Unique private Emote emotes$emote;
    @Unique private float emotes$time;
    @Unique private int emotes$ears, emotes$tail, emotes$pet;
    @Unique private BlockState emotes$disguise;

    @Override public void emotes$set(Emote emote, float time) { this.emotes$emote = emote; this.emotes$time = time; }
    @Override public void emotes$setCat(int ears, int tail) { this.emotes$ears = ears; this.emotes$tail = tail; }
    @Override public void emotes$setExtras(BlockState disguise, int pet) { this.emotes$disguise = disguise; this.emotes$pet = pet; }
    @Override public Emote emotes$getEmote() { return this.emotes$emote; }
    @Override public float emotes$getTime() { return this.emotes$time; }
    @Override public int emotes$earStyle() { return this.emotes$ears; }
    @Override public int emotes$tailStyle() { return this.emotes$tail; }
    @Override public BlockState emotes$disguise() { return this.emotes$disguise; }
    @Override public int emotes$pet() { return this.emotes$pet; }
}
