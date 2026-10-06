package com.example.emotewheel.client.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.example.emotewheel.client.EmoteStateHolder;
import com.example.emotewheel.client.MaidUtil;
import com.example.emotewheel.emote.Anim;
import com.example.emotewheel.emote.Emote;
import com.example.emotewheel.emote.Flavor;
import com.example.emotewheel.emote.Pose;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;

/**
 * Everything that touches the humanoid models:
 *  - maid helmet ears (extra cubes on the head that only show where the maid armor texture paints them),
 *  - Hinata outfit: thinner sleeves,
 *  - emotes: applies the current pose on top of the normal animation (player body AND armor),
 *  - cat ears + tail visibility on the player body.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {

    @Shadow @Final public ModelPart head;
    @Shadow @Final public ModelPart body;
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart rightLeg;
    @Shadow @Final public ModelPart leftLeg;

    @Unique private static final Pose emotes$POSE = new Pose();
    @Unique private boolean emotes$dirty;

    @Unique private static final String[] EAR_R = { "", "emote_ear1_r", "emote_ear2_r", "emote_ear3_r", "emote_ear4_r" };
    @Unique private static final String[] EAR_L = { "", "emote_ear1_l", "emote_ear2_l", "emote_ear3_l", "emote_ear4_l" };
    @Unique private static final String[] TAIL = { "", "emote_tail1", "emote_tail2", "emote_tail3" };
    @Unique private static final String[] PET = { "", "emote_pet1", "emote_pet2", "emote_pet3" };

    // Hinata outfit sleeve thickness: 1.0 = normal. Lower = thinner.
    @Unique private static final float SLIM = 0.7f;

    // ------------------------------------------------------------------ maid helmet ears (armor texture strip at 56..64 x 16..28)
    @Inject(method = "createMesh(Lnet/minecraft/client/model/geom/builders/CubeDeformation;F)Lnet/minecraft/client/model/geom/builders/MeshDefinition;",
            at = @At("RETURN"))
    private static void emotes$addMaidEars(CubeDeformation deformation, float yOffset, CallbackInfoReturnable<MeshDefinition> cir) {
        PartDefinition headDef = cir.getReturnValue().getRoot().getChild("head");
        CubeDeformation big = new CubeDeformation(0.75f);
        CubeDeformation tipD = new CubeDeformation(0.6f);
        headDef.addOrReplaceChild("maid_ear_r", CubeListBuilder.create()
            .texOffs(56, 16).addBox(-4.0f, -10.0f, -2.0f, 3.0f, 2.0f, 1.0f, big)
            .texOffs(56, 19).addBox(-3.0f, -13.0f, -2.0f, 1.0f, 2.0f, 1.0f, tipD), PartPose.ZERO);
        headDef.addOrReplaceChild("maid_ear_l", CubeListBuilder.create()
            .texOffs(56, 22).addBox(1.0f, -10.0f, -2.0f, 3.0f, 2.0f, 1.0f, big)
            .texOffs(56, 25).addBox(2.0f, -13.0f, -2.0f, 1.0f, 2.0f, 1.0f, tipD), PartPose.ZERO);
    }

    // ------------------------------------------------------------------ per-frame
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("RETURN"))
    private void emotes$setupAnim(HumanoidRenderState state, CallbackInfo ci) {
        boolean isPlayer = (Object) this instanceof PlayerModel;
        EmoteStateHolder holder = (state instanceof EmoteStateHolder h) ? h : null;

        if (isPlayer) {
            // Maid ears belong to the ARMOR only; the body model must never draw them (that was the "ears stay on" bug).
            emotes$show(head, "maid_ear_r", false);
            emotes$show(head, "maid_ear_l", false);

            int ear = holder != null ? holder.emotes$earStyle() : 0;
            int tail = holder != null ? holder.emotes$tailStyle() : 0;
            int pet = holder != null ? holder.emotes$pet() : 0;
            for (int i = 1; i <= 4; i++) {
                emotes$show(head, EAR_R[i], false); // drawn later with the look atlas, not hair
                emotes$show(head, EAR_L[i], false);
            }
            for (int i = 1; i <= 3; i++) {
                emotes$show(body, TAIL[i], false);
                emotes$show(head, PET[i], false);
            }
            float now = (System.nanoTime() / 1_000_000L % 1_000_000L) / 1000f;
            if (tail > 0 && body.hasChild(TAIL[tail])) {
                ModelPart tp = body.getChild(TAIL[tail]);
                tp.zRot = (float) Math.sin(now * 2.6f) * 0.35f;
            }
            if (pet > 0 && head.hasChild(PET[pet])) emotes$animatePet(head.getChild(PET[pet]), now, holder.emotes$getEmote() != null);
        } else {
            emotes$show(head, "maid_ear_r", true); // shows only where the armor texture paints them
            emotes$show(head, "maid_ear_l", true);

            // Hinata outfit: thinner sleeves (armor models only, never the player's own arms)
            Component name = state.chestEquipment.get(DataComponents.CUSTOM_NAME);
            float s = (name != null && "Hinata".equals(name.getString())) ? SLIM : 1.0f;
            rightArm.xScale = s; rightArm.zScale = s;
            leftArm.xScale = s;  leftArm.zScale = s;
        }

        emotes$applyEmote(state, holder);
    }

    /** Idle / look around / wash / lay down / sleep (closed-eye texture). Excited when you emote. */
    @Unique
    private static void emotes$animatePet(ModelPart pet, float now, boolean excited) {
        float speed = excited ? 1.7f : 1f;
        ModelPart body = pet.getChild("pet_body"), head = pet.getChild("pet_head"), tail = pet.getChild("pet_tail");
        ModelPart earL = head.getChild("pet_ear_l"), earR = head.getChild("pet_ear_r"), paw = pet.getChild("pet_paw_r");
        ModelPart eyesOpen = head.hasChild("pet_eyes_open") ? head.getChild("pet_eyes_open") : null;
        ModelPart eyesShut = head.hasChild("pet_eyes_shut") ? head.getChild("pet_eyes_shut") : null;

        float cycle = now % 22f;
        boolean laying = !excited && cycle > 10f && cycle < 20f;
        boolean sleeping = !excited && cycle > 13f && cycle < 19f;
        if (eyesOpen != null) eyesOpen.visible = !sleeping;
        if (eyesShut != null) eyesShut.visible = sleeping;

        float flatten = laying ? 0.55f : 1f;
        body.yScale = flatten + 0.05f * (float) Math.sin(now * (sleeping ? 1.1f : 2.2f) * speed);
        body.zScale = laying ? 1.25f : 1f;
        head.yRot = sleeping ? 0.15f : (float) Math.sin(now * 0.7f) * 0.5f;
        head.xRot = sleeping ? 0.45f : (float) Math.sin(now * 1.1f) * 0.1f + (laying ? 0.35f : 0f);
        head.zRot = sleeping ? 0.08f : (float) Math.sin(now * 0.4f) * 0.12f;
        tail.zRot = sleeping ? 0.05f : (float) Math.sin(now * 4.5f * speed) * 0.5f;
        tail.xRot = laying ? 0.4f : 0f;

        float tl = now % 4.3f, tr = (now + 2.1f) % 5.1f;
        float flickL = (!sleeping && tl < 0.25f) ? (float) Math.sin(tl / 0.25f * Math.PI * 3) * 0.5f : 0f;
        float flickR = (!sleeping && tr < 0.25f) ? (float) Math.sin(tr / 0.25f * Math.PI * 3) * -0.5f : 0f;
        earL.zRot = (sleeping ? 0.55f : 0.2f) + flickL;
        earR.zRot = (sleeping ? -0.55f : -0.2f) + flickR;
        earL.xRot = sleeping ? 0.35f : 0f;
        earR.xRot = sleeping ? 0.35f : 0f;

        float w = now % 9f;
        float wash = (!laying && w < 1.6f) ? (float) Math.sin(Math.PI * w / 1.6f) : 0f;
        paw.xRot = -1.9f * wash;
        head.xRot += 0.35f * wash;

        pet.y = (laying ? -7.2f : -8f) - (excited ? Math.abs((float) Math.sin(now * 7f)) * 0.6f : 0f);
        pet.xRot = laying ? 0.55f : 0f;
    }

    @Unique
    private static void emotes$show(ModelPart parent, String child, boolean visible) {
        if (parent.hasChild(child)) parent.getChild(child).visible = visible;
    }

    @Unique
    private void emotes$applyEmote(HumanoidRenderState state, EmoteStateHolder holder) {
        ModelPart root = ((Model) (Object) this).root();
        Emote emote = holder != null ? holder.emotes$getEmote() : null;

        if (emote == null) {
            if (emotes$dirty) { // an emote just ended: put the whole-body transform back
                root.x = 0; root.y = 0; root.z = 0;
                root.xRot = 0; root.yRot = 0; root.zRot = 0;
                emotes$dirty = false;
            }
            return;
        }

        float t = holder.emotes$getTime();
        Pose p = emotes$POSE;
        p.reset();
        emote.fn().apply(p, t);
        Flavor.apply(p, emote, t); // per-emote personality so similar emotes don't move identically

        // ease in at the start, and out at the end of one-shot emotes
        float w = Anim.smooth(t / 0.18f) * p.fade;
        if (emote.isOneShot()) w *= 1f - Anim.smooth((t - (emote.duration() - 0.2f)) / 0.2f);
        if (w <= 0f) return;

        boolean upperOnly = emote.isWalk(); // walking emotes leave the legs and body position to the game

        // Lean the torso forward/back about the hips: head, body and shoulders swing round the hip joint.
        float lean = p.lean * w;
        if (lean != 0f) {
            float c = (float) Math.cos(lean), s = (float) Math.sin(lean);
            emotes$swing(head, c, s);
            emotes$swing(body, c, s);
            emotes$swing(rightArm, c, s);
            emotes$swing(leftArm, c, s);
        }

        head.xRot += p.headX * w + lean;
        head.yRot += p.headY * w;
        head.zRot += p.headZ * w;

        body.xRot = emotes$mix(body.xRot, p.bodyX + p.lean, w);
        body.yRot = emotes$mix(body.yRot, p.bodyY, w);
        body.zRot = emotes$mix(body.zRot, p.bodyZ, w);

        rightArm.xRot = emotes$mix(rightArm.xRot, p.rax, w);
        rightArm.yRot = emotes$mix(rightArm.yRot, p.ray, w);
        rightArm.zRot = emotes$mix(rightArm.zRot, p.raz, w);
        leftArm.xRot = emotes$mix(leftArm.xRot, p.lax, w);
        leftArm.yRot = emotes$mix(leftArm.yRot, p.lay, w);
        leftArm.zRot = emotes$mix(leftArm.zRot, p.laz, w);

        if (upperOnly) return;

        rightLeg.xRot = emotes$mix(rightLeg.xRot, p.rlx, w);
        rightLeg.yRot = emotes$mix(rightLeg.yRot, p.rly, w);
        rightLeg.zRot = emotes$mix(rightLeg.zRot, p.rlz, w);
        leftLeg.xRot = emotes$mix(leftLeg.xRot, p.llx, w);
        leftLeg.yRot = emotes$mix(leftLeg.yRot, p.lly, w);
        leftLeg.zRot = emotes$mix(leftLeg.zRot, p.llz, w);

        // Whole-body transform. Flips and rolls pivot around the hips (12px below the neck).
        float flip = p.flip * w, roll = p.roll * w;
        root.xRot = flip;
        root.yRot = p.spin * w;
        root.zRot = roll;
        root.x = p.rootX * w + 12f * (float) Math.sin(roll);
        root.y = p.rootY * w + (12f - 12f * (float) Math.cos(flip)) + (12f - 12f * (float) Math.cos(roll));
        root.z = p.rootZ * w - 12f * (float) Math.sin(flip);
        emotes$dirty = true;
    }

    /** Rotate a part's position about the hip joint (y = 12, z = 0) around the x axis. */
    @Unique
    private static void emotes$swing(ModelPart part, float c, float s) {
        float y0 = part.y - 12f, z0 = part.z;
        part.y = 12f + y0 * c - z0 * s;
        part.z = y0 * s + z0 * c;
    }

    @Unique
    private static float emotes$mix(float a, float b, float w) {
        return a + (b - a) * w;
    }
}
