package com.example.emotewheel.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.example.emotewheel.States;
import com.example.emotewheel.client.CloneEntity;
import com.example.emotewheel.client.CloneManager;
import com.example.emotewheel.client.ClientStates;
import com.example.emotewheel.client.EmoteClient;
import com.example.emotewheel.client.EmoteConfig;
import com.example.emotewheel.client.EmoteProps;
import com.example.emotewheel.client.EmoteStateHolder;
import com.example.emotewheel.client.LookParts;
import com.example.emotewheel.client.MaidUtil;
import com.example.emotewheel.emote.Emote;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * - after a player's render state is filled in: which emote, which ear/tail style, block disguise, head pet
 * - Substitution Jutsu: draw a block instead of the player
 * - signature jutsu: draw their 3D props after the body
 * - Stealth: no nametag
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @org.spongepowered.asm.mixin.Shadow public abstract EntityModel<?> getModel();

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("RETURN"))
    private void emotes$extract(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        if (!(state instanceof EmoteStateHolder holder)) return;
        holder.emotes$set(null, 0f);
        holder.emotes$setCat(0, 0);
        holder.emotes$setExtras(null, 0);
        holder.emotes$setSelf(false);
        if (!(entity instanceof AbstractClientPlayer player)) return;

        Minecraft mc = Minecraft.getInstance();
        EmoteConfig cfg = EmoteConfig.get();
        boolean self = player == mc.player;
        holder.emotes$setSelf(self);
        AbstractClientPlayer who = player instanceof CloneEntity c ? c.owner() : player; // clones copy their owner

        Emote emote = null;
        float time = 0f;
        if (self && EmoteClient.isPreviewing()) {
            emote = EmoteClient.previewEmote();
            time = EmoteClient.previewTime();
        } else if (EmoteClient.isPreviewing()) {
            return; // the menu preview only draws the local player
        } else if (self || cfg.showOthers) {
            var source = CloneManager.emoteSource(player);
            emote = EmoteClient.current(source);
            if (emote != null) time = EmoteClient.time(source);
        }
        holder.emotes$set(emote, time);

        // cat ears + tail on the body (the maid helmet brings its own ears, so skip ours then)
        boolean enabled = emote != null && emote.isCat();
        int ear = 1, tail = 1; // other players always show the classic cat look
        if (self) {
            ear = cfg.earStyle; tail = cfg.tailStyle;
            if (cfg.catMode == 0) enabled = false;
            else if (cfg.catMode == 2) enabled = true;
        }
        boolean maidHelmet = MaidUtil.isMaidName(player.getItemBySlot(EquipmentSlot.HEAD).get(DataComponents.CUSTOM_NAME));
        holder.emotes$setCat(enabled && !maidHelmet ? ear : 0, enabled ? tail : 0);

        // Substitution Jutsu: become the block in the off hand (anything that isn't a plain block becomes an oak log)
        BlockState disguise = null;
        if (!EmoteClient.isPreviewing()
            && ClientStates.has(who.getUUID(), States.SUBSTITUTION)
            && States.allowed(States.SUBSTITUTION, who.getName().getString())) {
            var off = who.getOffhandItem();
            BlockState bs = off.getItem() instanceof BlockItem bi ? bi.getBlock().defaultBlockState() : Blocks.OAK_LOG.defaultBlockState();
            disguise = bs.getRenderShape() == RenderShape.MODEL ? bs : Blocks.OAK_LOG.defaultBlockState();
        }
        holder.emotes$setExtras(disguise, self && disguise == null ? cfg.pet : 0);
    }

    /** Substitution Jutsu: replace the whole player (body, held items, nametag, shadow) with the block. */
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("HEAD"), cancellable = true)
    private void emotes$submitDisguise(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                                       CameraRenderState camera, CallbackInfo ci) {
        if (state instanceof EmoteStateHolder h && h.emotes$disguise() != null) {
            poseStack.pushPose();
            poseStack.translate(-0.5, 0.0, -0.5);
            collector.submitBlock(poseStack, h.emotes$disguise(), state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
            ci.cancel();
        }
    }

    /** The signature jutsu get real 3D orbs and beams, drawn after the body. */
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("TAIL"))
    private void emotes$submitProps(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                                    CameraRenderState camera, CallbackInfo ci) {
        if (!(state instanceof EmoteStateHolder h)) return;
        Emote e = h.emotes$getEmote();
        if (e != null && EmoteProps.has(e)) EmoteProps.submit(e, h.emotes$getTime(), state.bodyRot, poseStack, collector);
        EntityModel<?> model = getModel();
        if (model instanceof PlayerModel<?> pm && (h.emotes$earStyle() > 0 || h.emotes$tailStyle() > 0 || h.emotes$pet() > 0)) {
            LookParts.submit(pm, state, poseStack, collector, LookParts.textureFor(h.emotes$isSelf()),
                h.emotes$earStyle(), h.emotes$tailStyle(), h.emotes$pet());
        }
    }

    /** Stealth: no nametag floating over the owner (this is what gives the real one away among shadow clones). */
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("RETURN"), cancellable = true)
    private void emotes$stealthName(LivingEntity entity, double distSq, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && entity instanceof AbstractClientPlayer p
            && ClientStates.has(p.getUUID(), States.STEALTH)
            && States.allowed(States.STEALTH, p.getName().getString())) {
            cir.setReturnValue(false);
        }
    }
}
