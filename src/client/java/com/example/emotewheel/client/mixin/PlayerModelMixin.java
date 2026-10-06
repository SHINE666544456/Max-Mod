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
 * Extra cubes for ear styles, tails and the head pet.
 * UVs sample the dedicated look atlas (fur / inner / belly / dark / tail / eyes), not the player's hair.
 *
 * Part names:  head: emote_ear{1..4}_{r,l}, emote_pet{1..3}   body: emote_tail{1..3}
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {

    @Inject(method = "createMesh(Lnet/minecraft/client/model/geom/builders/CubeDeformation;Z)Lnet/minecraft/client/model/geom/builders/MeshDefinition;",
            at = @At("RETURN"), require = 0)
    private static void emotes$addExtras(CubeDeformation deformation, boolean slim, CallbackInfoReturnable<MeshDefinition> cir) {
        PartDefinition root = cir.getReturnValue().getRoot();
        PartDefinition head = root.getChild("head");
        PartDefinition body = root.getChild("body");
        for (int side = -1; side <= 1; side += 2) {
            String s = side < 0 ? "_r" : "_l";
            addCatEar(head, "emote_ear1" + s, side);
            addFoxEar(head, "emote_ear2" + s, side);
            addBunnyEar(head, "emote_ear3" + s, side);
            addBearEar(head, "emote_ear4" + s, side);
        }
        addCatTail(body);
        addFoxTail(body);
        addBunnyTail(body);
        addPet(head, 1, 2f, 1f, 4f, 0.0f, 1f);
        addPet(head, 2, 3f, 1f, 5f, 0.25f, 2f);
        addPet(head, 3, 5f, 1f, 1.6f, 0.5f, 1f);
    }

    private static void addCatEar(PartDefinition head, String name, int side) {
        CubeDeformation big = new CubeDeformation(0.75f), tip = new CubeDeformation(0.6f);
        float x0 = side < 0 ? -4f : 1f, tx = side < 0 ? -3f : 2f;
        PartDefinition ear = head.addOrReplaceChild(name, CubeListBuilder.create()
            .texOffs(0, 0).addBox(x0, -10f, -2f, 3f, 2f, 1f, big)
            .texOffs(48, 0).addBox(tx, -13f, -2f, 1f, 2f, 1f, tip), PartPose.ZERO);
        ear.addOrReplaceChild("inner", CubeListBuilder.create()
            .texOffs(16, 0).addBox(x0 + 0.45f, -9.7f, -2.35f, 2.1f, 1.3f, 0.4f), PartPose.ZERO);
    }

    private static void addFoxEar(PartDefinition head, String name, int side) {
        CubeDeformation big = new CubeDeformation(0.6f), tip = new CubeDeformation(0.5f);
        float x0 = side < 0 ? -4f : 1f, tx = side < 0 ? -3f : 2f;
        PartDefinition ear = head.addOrReplaceChild(name, CubeListBuilder.create()
            .texOffs(0, 0).addBox(x0, -11f, -2f, 3f, 3f, 1f, big)
            .texOffs(48, 0).addBox(tx, -15f, -2f, 1f, 3f, 1f, tip), PartPose.ZERO);
        ear.addOrReplaceChild("inner", CubeListBuilder.create()
            .texOffs(16, 0).addBox(x0 + 0.5f, -10.4f, -2.3f, 2f, 2.1f, 0.35f), PartPose.ZERO);
    }

    private static void addBunnyEar(PartDefinition head, String name, int side) {
        PartDefinition ear = head.addOrReplaceChild(name, CubeListBuilder.create()
            .texOffs(0, 0).addBox(-1f, -7f, -0.5f, 2f, 7f, 1f, new CubeDeformation(0.25f)),
            PartPose.offsetAndRotation(side * 2.2f, -8f, -1.5f, 0f, 0f, side * 0.25f));
        ear.addOrReplaceChild("inner", CubeListBuilder.create()
            .texOffs(16, 0).addBox(-0.55f, -6.2f, -0.72f, 1.1f, 5.4f, 0.35f), PartPose.ZERO);
    }

    private static void addBearEar(PartDefinition head, String name, int side) {
        float x0 = side < 0 ? -5f : 2f;
        PartDefinition ear = head.addOrReplaceChild(name, CubeListBuilder.create()
            .texOffs(0, 0).addBox(x0, -9.5f, -1f, 3f, 2f, 2f, new CubeDeformation(0.35f)), PartPose.ZERO);
        ear.addOrReplaceChild("inner", CubeListBuilder.create()
            .texOffs(16, 0).addBox(x0 + 0.55f, -9.1f, -1.35f, 1.9f, 1.2f, 0.4f), PartPose.ZERO);
    }

    private static void addCatTail(PartDefinition body) {
        PartDefinition t = body.addOrReplaceChild("emote_tail1", CubeListBuilder.create()
            .texOffs(0, 16).addBox(-1f, 0f, 0f, 2f, 5f, 2f), PartPose.offsetAndRotation(0f, 10f, 2f, 0.7f, 0f, 0f));
        t.addOrReplaceChild("tip", CubeListBuilder.create()
            .texOffs(48, 0).addBox(-1f, 0f, 0f, 2f, 5f, 2f), PartPose.offsetAndRotation(0f, 5f, 0f, 1.3f, 0f, 0f));
    }

    private static void addFoxTail(PartDefinition body) {
        PartDefinition t = body.addOrReplaceChild("emote_tail2", CubeListBuilder.create()
            .texOffs(0, 16).addBox(-1.5f, 0f, 0f, 3f, 5f, 3f, new CubeDeformation(0.2f)), PartPose.offsetAndRotation(0f, 9.5f, 2.2f, 0.55f, 0f, 0f));
        PartDefinition mid = t.addOrReplaceChild("mid", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-2f, 0f, 0f, 4f, 5f, 4f, new CubeDeformation(0.3f)), PartPose.offsetAndRotation(0f, 5f, -0.3f, 0.5f, 0f, 0f));
        mid.addOrReplaceChild("tip", CubeListBuilder.create()
            .texOffs(32, 0).addBox(-1.5f, 0f, 0f, 3f, 3f, 3f), PartPose.offsetAndRotation(0f, 5f, 0.3f, 0.3f, 0f, 0f));
    }

    private static void addBunnyTail(PartDefinition body) {
        body.addOrReplaceChild("emote_tail3", CubeListBuilder.create()
            .texOffs(32, 0).addBox(-1.5f, -1.5f, 0f, 3f, 3f, 2f, new CubeDeformation(0.5f)), PartPose.offset(0f, 9.5f, 2.2f));
    }

    private static void addPet(PartDefinition head, int kind, float earH, float earW, float tailLen, float tailFluff, float muzzle) {
        CubeDeformation fluff = new CubeDeformation(tailFluff);
        PartDefinition pet = head.addOrReplaceChild("emote_pet" + kind, CubeListBuilder.create(), PartPose.offset(0f, -8f, 0f));
        pet.addOrReplaceChild("pet_body", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-2f, -3f, -1.5f, 4f, 3f, 3f)
            .texOffs(32, 0).addBox(-1.6f, -1.6f, -1.7f, 3.2f, 1.4f, 1.2f), PartPose.offset(0f, 0f, 1f));
        PartDefinition ph = pet.addOrReplaceChild("pet_head", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-1.5f, -3f, -1.5f, 3f, 3f, 3f), PartPose.offset(0f, -2.6f, -1.2f));
        ph.addOrReplaceChild("pet_muzzle", CubeListBuilder.create()
            .texOffs(32, 0).addBox(-1f, -1f, -1f - muzzle, 2f, 1f, 1f + muzzle)
            .texOffs(48, 16).addBox(-0.4f, -0.55f, -1.15f - muzzle, 0.8f, 0.45f, 0.5f), PartPose.offset(0f, -0.2f, -1.4f));
        ph.addOrReplaceChild("pet_ear_l", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-0.5f, -earH, -0.5f, earW, earH, 1f)
            .texOffs(16, 0).addBox(-0.25f, -earH + 0.3f, -0.72f, Math.max(0.5f, earW - 0.5f), earH - 0.6f, 0.35f),
            PartPose.offsetAndRotation(1.0f, -3f, 0f, 0f, 0f, 0.2f));
        ph.addOrReplaceChild("pet_ear_r", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-earW + 0.5f, -earH, -0.5f, earW, earH, 1f)
            .texOffs(16, 0).addBox(-earW + 0.75f, -earH + 0.3f, -0.72f, Math.max(0.5f, earW - 0.5f), earH - 0.6f, 0.35f),
            PartPose.offsetAndRotation(-1.0f, -3f, 0f, 0f, 0f, -0.2f));
        ph.addOrReplaceChild("pet_eyes_open", CubeListBuilder.create()
            .texOffs(16, 16).addBox(-1.15f, -2.15f, -1.62f, 0.9f, 0.7f, 0.2f)
            .texOffs(16, 16).addBox(0.25f, -2.15f, -1.62f, 0.9f, 0.7f, 0.2f), PartPose.ZERO);
        ph.addOrReplaceChild("pet_eyes_shut", CubeListBuilder.create()
            .texOffs(32, 16).addBox(-1.15f, -1.95f, -1.62f, 0.9f, 0.35f, 0.2f)
            .texOffs(32, 16).addBox(0.25f, -1.95f, -1.62f, 0.9f, 0.35f, 0.2f), PartPose.ZERO);
        pet.addOrReplaceChild("pet_tail", CubeListBuilder.create()
            .texOffs(0, 16).addBox(-0.5f, -tailLen, -0.5f, 1f, tailLen, 1f, fluff), PartPose.offsetAndRotation(0f, -0.5f, 2.6f, -0.5f, 0f, 0f));
        pet.addOrReplaceChild("pet_paw_l", CubeListBuilder.create()
            .texOffs(48, 16).addBox(-0.5f, -1f, -0.5f, 1f, 1f, 1f), PartPose.offset(1.0f, 0f, -1.8f));
        pet.addOrReplaceChild("pet_paw_r", CubeListBuilder.create()
            .texOffs(48, 16).addBox(-0.5f, -1f, -0.5f, 1f, 1f, 1f), PartPose.offset(-1.0f, 0f, -1.8f));
    }
}
