package com.example.emotewheel.client;

import com.example.emotewheel.emote.Rig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import org.joml.Quaternionf;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Draws the player's ears, tail and head pet. They live in their own always-visible model (LookMeshes), are textured
 * from the look atlas (your hex colours), and are placed using the player's real head/body pose (Rig), so they follow
 * nods, tilts, leaning, flips and spins exactly.
 */
public final class LookParts {
    private LookParts() {}

    private static final String[] EAR_R = { "", "emote_ear1_r", "emote_ear2_r", "emote_ear3_r", "emote_ear4_r" };
    private static final String[] EAR_L = { "", "emote_ear1_l", "emote_ear2_l", "emote_ear3_l", "emote_ear4_l" };
    private static final String[] TAIL = { "", "emote_tail1", "emote_tail2", "emote_tail3" };
    private static final String[] PET = { "", "emote_pet1", "emote_pet2", "emote_pet3" };

    private static ModelPart root;

    private static ModelPart root() {
        if (root == null) root = LookMeshes.bakeRoot();
        return root;
    }

    public static void submit(EmoteStateHolder h, LivingEntityRenderState state, PoseStack ps, SubmitNodeCollector c) {
        int ear = h.emotes$earStyle(), tail = h.emotes$tailStyle(), pet = h.emotes$pet();
        float[] rig = h.emotes$rig();
        if ((ear <= 0 && tail <= 0 && pet <= 0) || rig == null) return;

        ModelPart r = root();
        ModelPart head = r.getChild("head"), body = r.getChild("body");
        RenderType rt = RenderTypes.entityCutoutNoCull(texture(h.emotes$isSelf()));
        int light = state.lightCoords, overlay = OverlayTexture.NO_OVERLAY;
        float now = (System.nanoTime() / 1_000_000L % 1_000_000L) / 1000f; // same clock for everyone: no per-player state

        if (ear > 0 || pet > 0) {
            ps.pushPose();
            frame(ps, state, rig, Rig.HEAD);
            if (ear > 0) {
                animateEars(head.getChild(EAR_R[ear]), head.getChild(EAR_L[ear]), now);
                c.submitModelPart(head.getChild(EAR_R[ear]), ps, rt, light, overlay, null);
                c.submitModelPart(head.getChild(EAR_L[ear]), ps, rt, light, overlay, null);
            }
            if (pet > 0) {
                ModelPart p = head.getChild(PET[pet]);
                animatePet(p, now, h.emotes$isSelf() && h.emotes$getEmote() != null);
                c.submitModelPart(p, ps, rt, light, overlay, null);
            }
            ps.popPose();
        }
        if (tail > 0) {
            ps.pushPose();
            frame(ps, state, rig, Rig.BODY);
            ModelPart t = body.getChild(TAIL[tail]);
            t.zRot = (float) Math.sin(now * 2.6f) * 0.35f; // lazy swish
            c.submitModelPart(t, ps, rt, light, overlay, null);
            ps.popPose();
        }
    }

    /** Put the pose stack where the head (or body) is: entity frame, then the whole-model root, then that part. */
    private static void frame(PoseStack ps, LivingEntityRenderState state, float[] rig, int part) {
        ps.mulPose(Axis.YP.rotationDegrees(180f - state.bodyRot));
        ps.scale(-1f, -1f, 1f);
        ps.translate(0f, -1.501f, 0f);
        node(ps, rig, Rig.ROOT);
        node(ps, rig, part);
    }

    private static void node(PoseStack ps, float[] r, int o) {
        ps.translate(r[o] / 16f, r[o + 1] / 16f, r[o + 2] / 16f);
        if (r[o + 3] != 0f || r[o + 4] != 0f || r[o + 5] != 0f) {
            ps.mulPose(new Quaternionf().rotationZYX(r[o + 5], r[o + 4], r[o + 3]));
        }
    }

    private static net.minecraft.resources.Identifier texture(boolean self) {
        LookTextures.rebuild();
        return self ? LookTextures.self() : LookTextures.others();
    }

    private static void animateEars(ModelPart earR, ModelPart earL, float now) {
        float tl = now % 4.3f, tr = (now + 2.1f) % 5.1f; // quick, independent flicks
        earL.zRot = tl < 0.25f ? (float) Math.sin(tl / 0.25f * Math.PI * 3) * 0.18f : 0f;
        earR.zRot = tr < 0.25f ? (float) Math.sin(tr / 0.25f * Math.PI * 3) * -0.18f : 0f;
    }

    /**
     * The pet's day (22 s loop): looking around, a paw wash, a stretch, a yawn, then it lies down and falls asleep with
     * its eyes shut. When you emote it gets excited: it stands up, hops and wags.
     */
    private static void animatePet(ModelPart pet, float now, boolean excited) {
        float speed = excited ? 1.7f : 1f;
        ModelPart body = pet.getChild("pet_body"), head = pet.getChild("pet_head"), tail = pet.getChild("pet_tail");
        ModelPart earL = head.getChild("pet_ear_l"), earR = head.getChild("pet_ear_r"), paw = pet.getChild("pet_paw_r");
        ModelPart eyesOpen = head.hasChild("pet_eyes_open") ? head.getChild("pet_eyes_open") : null;
        ModelPart eyesShut = head.hasChild("pet_eyes_shut") ? head.getChild("pet_eyes_shut") : null;

        float cycle = now % 22f;
        boolean laying = !excited && cycle > 10f && cycle < 20f;
        boolean sleeping = !excited && cycle > 13f && cycle < 19f;
        float st = (!excited && cycle > 4f && cycle < 6.2f) ? (float) Math.sin(Math.PI * (cycle - 4f) / 2.2f) : 0f;   // stretch
        float yw = (!excited && cycle > 7f && cycle < 8.6f) ? (float) Math.sin(Math.PI * (cycle - 7f) / 1.6f) : 0f;   // yawn
        // blink every few seconds (only when awake)
        boolean blink = !sleeping && ((now % 3.7f) < 0.12f);
        if (eyesOpen != null) eyesOpen.visible = !sleeping && !blink;
        if (eyesShut != null) eyesShut.visible = sleeping || blink;

        float flatten = laying ? 0.55f : 1f;
        body.yScale = flatten + 0.05f * (float) Math.sin(now * (sleeping ? 1.1f : 2.2f) * speed);
        body.zScale = laying ? 1.25f : 1f;
        head.yRot = sleeping ? 0.15f : (float) Math.sin(now * 0.7f) * 0.5f;
        head.xRot = sleeping ? 0.45f : (float) Math.sin(now * 1.1f) * 0.1f + (laying ? 0.35f : 0f) - 0.55f * yw + 0.45f * st;
        head.zRot = sleeping ? 0.08f : (float) Math.sin(now * 0.4f) * 0.12f;
        tail.zRot = sleeping ? 0.05f : (float) Math.sin(now * 4.5f * speed) * 0.5f;
        tail.xRot = (laying ? 0.4f : 0f) - 0.9f * st; // tail goes up during the stretch

        float tl = now % 4.3f, tr = (now + 2.1f) % 5.1f;
        float flickL = (!sleeping && tl < 0.25f) ? (float) Math.sin(tl / 0.25f * Math.PI * 3) * 0.5f : 0f;
        float flickR = (!sleeping && tr < 0.25f) ? (float) Math.sin(tr / 0.25f * Math.PI * 3) * -0.5f : 0f;
        earL.zRot = (sleeping ? 0.55f : 0.2f) + flickL + 0.3f * yw;
        earR.zRot = (sleeping ? -0.55f : -0.2f) - flickR * 0 + flickR - 0.3f * yw;
        earL.xRot = sleeping ? 0.35f : 0f;
        earR.xRot = sleeping ? 0.35f : 0f;

        float w = now % 9f;
        float wash = (!laying && st == 0f && w < 1.6f) ? (float) Math.sin(Math.PI * w / 1.6f) : 0f;
        paw.xRot = -1.9f * wash;
        head.xRot += 0.35f * wash;

        pet.y = (laying ? -7.2f : -8f) - (excited ? Math.abs((float) Math.sin(now * 7f)) * 0.6f : 0f);
        pet.xRot = (laying ? 0.55f : 0f) + 0.35f * st; // front dips for the stretch
    }
}
