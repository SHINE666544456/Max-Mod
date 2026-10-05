package com.example.emotewheel.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.emotewheel.client.CloneManager;
import com.example.emotewheel.client.EmoteClient;
import com.example.emotewheel.client.EmoteConfig;
import com.example.emotewheel.client.EmoteStateHolder;
import com.example.emotewheel.client.MaidUtil;
import com.example.emotewheel.emote.Emote;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/** After a living entity's render state is filled in, record which emote (if any) that player is playing and which cat parts to show. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("RETURN"))
    private void emotes$extract(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        if (!(state instanceof EmoteStateHolder holder)) return;
        holder.emotes$set(null, 0f);
        holder.emotes$setCat(false, false);
        if (!(entity instanceof AbstractClientPlayer player)) return;

        Minecraft mc = Minecraft.getInstance();
        EmoteConfig cfg = EmoteConfig.get();
        boolean self = player == mc.player;

        Emote emote = null;
        float time = 0f;
        if (self && EmoteClient.isPreviewing()) {
            emote = EmoteClient.previewEmote();
            time = EmoteClient.previewTime();
        } else if (EmoteClient.isPreviewing()) {
            return; // the menu preview only draws the local player
        } else if (self || cfg.showOthers) {
            var source = CloneManager.emoteSource(player); // clones copy their owner's emote
            emote = EmoteClient.current(source);
            if (emote != null) time = EmoteClient.time(source);
        }
        holder.emotes$set(emote, time);

        // cat ears + tail on the player body (the maid helmet brings its own ears, so skip ours then)
        boolean enabled = emote != null && emote.isCat();
        if (self) {
            if (cfg.catMode == 0) enabled = false;
            else if (cfg.catMode == 2) enabled = true;
        }
        boolean maidHelmet = MaidUtil.isMaidName(player.getItemBySlot(EquipmentSlot.HEAD).get(DataComponents.CUSTOM_NAME));
        holder.emotes$setCat(enabled && !maidHelmet, enabled);
    }
}
