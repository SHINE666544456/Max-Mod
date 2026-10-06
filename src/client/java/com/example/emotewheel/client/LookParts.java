package com.example.emotewheel.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Draws ear / tail / pet cubes with the look atlas instead of the player's hair. */
public final class LookParts {
    private LookParts() {}

    private static final String[] EAR_R = { "", "emote_ear1_r", "emote_ear2_r", "emote_ear3_r", "emote_ear4_r" };
    private static final String[] EAR_L = { "", "emote_ear1_l", "emote_ear2_l", "emote_ear3_l", "emote_ear4_l" };
    private static final String[] TAIL = { "", "emote_tail1", "emote_tail2", "emote_tail3" };
    private static final String[] PET = { "", "emote_pet1", "emote_pet2", "emote_pet3" };

    public static void submit(PlayerModel model, LivingEntityRenderState state, PoseStack ps,
                              SubmitNodeCollector collector, Identifier tex, int ear, int tail, int pet) {
        RenderType rt = RenderTypes.entityCutoutNoCull(tex);
        int light = state.lightCoords;
        int overlay = OverlayTexture.NO_OVERLAY;
        ModelPart root = model.root();
        ModelPart head = model.head;
        ModelPart body = model.body;

        if (ear > 0) {
            submitChild(ps, collector, rt, light, overlay, root, head, named(head, EAR_R[ear]));
            submitChild(ps, collector, rt, light, overlay, root, head, named(head, EAR_L[ear]));
        }
        if (tail > 0) submitChild(ps, collector, rt, light, overlay, root, body, named(body, TAIL[tail]));
        if (pet > 0) submitChild(ps, collector, rt, light, overlay, root, head, named(head, PET[pet]));
    }

    private static ModelPart named(ModelPart parent, String name) {
        return parent.hasChild(name) ? parent.getChild(name) : null;
    }

    private static void submitChild(PoseStack ps, SubmitNodeCollector collector, RenderType rt, int light, int overlay,
                                    ModelPart root, ModelPart parent, ModelPart part) {
        if (part == null) return;
        boolean vis = part.visible;
        part.visible = true;
        ps.pushPose();
        root.translateAndRotate(ps);
        parent.translateAndRotate(ps);
        collector.submitModelPart(part, ps, rt, light, overlay, null);
        ps.popPose();
        part.visible = vis;
    }

    public static Identifier textureFor(boolean self) {
        LookTextures.rebuild();
        return self ? LookTextures.self() : LookTextures.others();
    }
}

