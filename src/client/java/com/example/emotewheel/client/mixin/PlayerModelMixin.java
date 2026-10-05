package com.example.emotewheel.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;

/**
 * Adds cat ears and a tail to the player's own body model. They are textured from the top of the head in the skin,
 * so they match the hair colour. They stay hidden unless an emote (or the setting) turns them on.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {

    @Inject(method = "createMesh(Lnet/minecraft/client/model/geom/builders/CubeDeformation;Z)Lnet/minecraft/client/model/geom/builders/MeshDefinition;",
            at = @At("RETURN"), require = 0)
    private static void emotes$addCatParts(CubeDeformation deformation, boolean slim, CallbackInfoReturnable<MeshDefinition> cir) {
        PartDefinition root = cir.getReturnValue().getRoot();
        PartDefinition head = root.getChild("head");
        PartDefinition body = root.getChild("body");

        CubeDeformation big = new CubeDeformation(0.75f);
        CubeDeformation tipD = new CubeDeformation(0.6f);
        head.addOrReplaceChild("emote_cat_ear_r", CubeListBuilder.create()
            .texOffs(8, 0).addBox(-4.0f, -10.0f, -2.0f, 3.0f, 2.0f, 1.0f, big)
            .texOffs(8, 3).addBox(-3.0f, -13.0f, -2.0f, 1.0f, 2.0f, 1.0f, tipD), PartPose.ZERO);
        head.addOrReplaceChild("emote_cat_ear_l", CubeListBuilder.create()
            .texOffs(8, 0).addBox(1.0f, -10.0f, -2.0f, 3.0f, 2.0f, 1.0f, big)
            .texOffs(8, 3).addBox(2.0f, -13.0f, -2.0f, 1.0f, 2.0f, 1.0f, tipD), PartPose.ZERO);

        // two-segment tail: out and down from the base of the spine, curling up at the tip
        PartDefinition tail = body.addOrReplaceChild("emote_cat_tail", CubeListBuilder.create()
            .texOffs(8, 0).addBox(-1.0f, 0.0f, 0.0f, 2.0f, 5.0f, 2.0f), PartPose.offsetAndRotation(0.0f, 10.0f, 2.0f, 0.7f, 0.0f, 0.0f));
        tail.addOrReplaceChild("emote_cat_tail_tip", CubeListBuilder.create()
            .texOffs(8, 0).addBox(-1.0f, 0.0f, 0.0f, 2.0f, 5.0f, 2.0f), PartPose.offsetAndRotation(0.0f, 5.0f, 0.0f, 1.3f, 0.0f, 0.0f));
    }
}
