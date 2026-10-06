package com.example.emotewheel.client;

import static com.example.emotewheel.emote.Anim.*;

import java.util.Set;

import com.example.emotewheel.emote.Emote;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Real 3D objects for the signature jutsu: spinning glowing orbs (built from vanilla blocks drawn at full brightness,
 * so they glow in the dark), crackling lightning arcs, and short beams. Drawn after the player's body.
 */
public final class EmoteProps {
    private EmoteProps() {}

    private static final Set<String> IDS = Set.of(
        "rasengan", "chidori", "kamehameha", "hollow_purple", "lapse_blue", "reversal_red",
        "fireball_jutsu", "spirit_gun", "zoltraak");

    private static final BlockState SEA = Blocks.SEA_LANTERN.defaultBlockState();
    private static final BlockState WHITE = Blocks.WHITE_CONCRETE.defaultBlockState();
    private static final BlockState LB_GLASS = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
    private static final BlockState LB_CONC = Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState();
    private static final BlockState BLUE_GLASS = Blocks.BLUE_STAINED_GLASS.defaultBlockState();
    private static final BlockState RED_CONC = Blocks.RED_CONCRETE.defaultBlockState();
    private static final BlockState RED_GLASS = Blocks.RED_STAINED_GLASS.defaultBlockState();
    private static final BlockState PURPLE_CONC = Blocks.PURPLE_CONCRETE.defaultBlockState();
    private static final BlockState MAGENTA_GLASS = Blocks.MAGENTA_STAINED_GLASS.defaultBlockState();
    private static final BlockState CRYING = Blocks.CRYING_OBSIDIAN.defaultBlockState();
    private static final BlockState MAGMA = Blocks.MAGMA_BLOCK.defaultBlockState();
    private static final BlockState ORANGE_GLASS = Blocks.ORANGE_STAINED_GLASS.defaultBlockState();

    /** One glowing orb: where it is (relative to the player's feet and facing), how big, and what it is made of. */
    private record Prop(double fwd, double right, double up, float size, BlockState core, BlockState shell, int orbiters, boolean arcs) {}

    private static final Prop[] NONE = new Prop[0];

    public static boolean has(Emote e) { return IDS.contains(e.id()); }

    // ------------------------------------------------------------------ what each jutsu looks like over time
    private static Prop[] props(String id, float t) {
        switch (id) {
            case "rasengan": {
                float g = smooth(t / 0.6f);
                return new Prop[] { new Prop(0.8, 0.30, 1.15, 0.32f * g, SEA, LB_GLASS, 4, false) };
            }
            case "chidori": {
                float g = smooth(t / 0.4f);
                return new Prop[] { new Prop(0.55, 0.30, 0.95, 0.26f * g, WHITE, LB_GLASS, 0, true) };
            }
            case "lapse_blue": {
                float g = smooth(t / 0.5f);
                return new Prop[] { new Prop(0.9, 0.18, 1.25, 0.34f * g, LB_CONC, BLUE_GLASS, 3, false) };
            }
            case "reversal_red": {
                float g = smooth(t / 0.5f);
                return new Prop[] { new Prop(0.9, -0.18, 1.25, 0.34f * g, RED_CONC, RED_GLASS, 3, false) };
            }
            case "kamehameha": {
                float ph = saw(t, 5f);
                float charge = seq(ph, 0f, 0f, 2.5f, 1f, 3.3f, 1f, 3.4f, 0f, 5f, 0f);
                float thrust = seq(ph, 0f, 0f, 3.2f, 0f, 3.5f, 1f, 4.6f, 1f, 5f, 0f);
                float m = Math.max(charge, thrust);
                if (m < 0.02f) return NONE;
                return new Prop[] { new Prop(lerp(0.45f, 0.9f, thrust), 0.0, lerp(0.95f, 1.2f, thrust), 0.12f + 0.38f * m, SEA, LB_GLASS, 3, false) };
            }
            case "hollow_purple": {
                // red + blue infinities form, drift together, smash, then the purple flies out
                float ph = saw(t, 5.6f);
                float form = seq(ph, 0f, 0f, 0.7f, 1f, 5.2f, 1f, 5.6f, 0f);
                float smash = seq(ph, 0f, 0f, 2.1f, 0f, 2.75f, 1f, 5.6f, 1f);
                float purple = seq(ph, 0f, 0f, 2.65f, 0f, 3.15f, 1f, 5.1f, 1f, 5.6f, 0f);
                if (purple < 0.18f) {
                    double spread = lerp(1.05f, 0.04f, smash);
                    float s = 0.24f * form * (1f + 0.12f * sin(t * 18f));
                    float spin = smash * 0.15f;
                    return new Prop[] {
                        new Prop(0.72 + spin, spread, 1.28, s, LB_CONC, BLUE_GLASS, 3, false),
                        new Prop(0.72 + spin, -spread, 1.28, s, RED_CONC, RED_GLASS, 3, false)
                    };
                }
                float big = 0.30f + 0.48f * purple;
                float fly = smooth((ph - 3.35f) / 1.5f) * 2.8f;
                return new Prop[] { new Prop(0.9 + fly, 0.0, 1.28, big, CRYING, MAGENTA_GLASS, 5, false) };
            }
            case "fireball_jutsu": {
                float size = seq(t, 0f, 0f, 2.2f, 0f, 2.6f, 0.35f, 3.4f, 0.9f, 3.9f, 0.5f, 4.4f, 0f);
                if (size < 0.02f) return NONE;
                float fly = smooth((t - 2.4f) / 1.4f) * 3.0f; // the fireball leaves your mouth and flies forward
                return new Prop[] { new Prop(0.7 + fly, 0.0, 1.5, size, MAGMA, ORANGE_GLASS, 3, false) };
            }
            case "spirit_gun": {
                float ph = saw(t, 1.6f);
                float charge = smooth((ph - 0.2f) / 0.7f) * (1f - smooth((ph - 0.95f) / 0.1f));
                return new Prop[] { new Prop(1.0, 0.35, 1.38, 0.06f + 0.14f * charge, SEA, LB_GLASS, 0, false) };
            }
            case "zoltraak": {
                float ph = saw(t, 3f);
                float charge = smooth((ph - 0.3f) / 1.6f) * (1f - smooth((ph - 2.0f) / 0.12f));
                return new Prop[] { new Prop(0.9, 0.2, 1.3, 0.1f + 0.3f * charge, PURPLE_CONC, MAGENTA_GLASS, 3, false) };
            }
            default:
                return NONE;
        }
    }

    /** Length (blocks) of the beam leaving the first orb at this moment, or 0. */
    private static float beamLen(String id, float t) {
        switch (id) {
            case "kamehameha": {
                float ph = saw(t, 5f);
                float thrust = seq(ph, 0f, 0f, 3.2f, 0f, 3.5f, 1f, 4.6f, 1f, 5f, 0f);
                return thrust < 0.05f ? 0f : 4.5f * smooth(thrust) * (1f - smooth((ph - 4.6f) / 0.4f));
            }
            case "spirit_gun": {
                float ph = saw(t, 1.6f);
                return 3.5f * smooth((ph - 0.93f) / 0.08f) * (1f - smooth((ph - 1.1f) / 0.15f));
            }
            case "zoltraak": {
                float ph = saw(t, 3f);
                return 5.0f * smooth((ph - 2.0f) / 0.12f) * (1f - smooth((ph - 2.6f) / 0.3f));
            }
            case "hollow_purple": {
                float ph = saw(t, 5.6f);
                float fire = seq(ph, 0f, 0f, 3.3f, 0f, 3.55f, 1f, 5.0f, 1f, 5.6f, 0f);
                return fire < 0.05f ? 0f : 3.8f * fire;
            }
            default:
                return 0f;
        }
    }

    // ------------------------------------------------------------------ drawing
    /** Offset from the player's feet, given a facing in degrees: forward, to their right, and up. */
    private static double[] at(float yawDeg, double fwd, double right, double up) {
        double th = Math.toRadians(yawDeg);
        double fx = -Math.sin(th), fz = Math.cos(th), rx = -Math.cos(th), rz = -Math.sin(th);
        return new double[] { fx * fwd + rx * right, up, fz * fwd + rz * right };
    }

    /** Called after the player's body has been drawn; the pose stack is at the player's feet, unrotated. */
    public static void submit(Emote e, float t, float yawDeg, PoseStack ps, SubmitNodeCollector c) {
        Prop[] props = props(e.id(), t);
        for (Prop p : props) {
            if (p.size() < 0.02f) continue;
            double[] o = at(yawDeg, p.fwd(), p.right(), p.up());
            float pulse = 1f + 0.06f * sin(t * 14f);
            cube(ps, c, p.core(), o, p.size() * 0.55f * pulse, t * 220f, t * 140f);
            cube(ps, c, p.shell(), o, p.size() * pulse, -t * 170f, t * 90f);
            for (int i = 0; i < p.orbiters(); i++) {
                float a = t * 5f + i * TAU / p.orbiters();
                double r = p.size() * 0.95;
                double[] q = { o[0] + cos(a) * r, o[1] + sin(a * 1.3f) * r * 0.5, o[2] + sin(a) * r };
                cube(ps, c, p.core(), q, p.size() * 0.18f, t * 300f, 0f);
            }
            if (p.arcs()) {
                int frame = (int) (t * 24f); // new random arcs every ~40ms: crackling
                for (int i = 0; i < 7; i++) {
                    int seed = frame * 13 + i * 31;
                    float len = p.size() * (1.2f + 0.9f * abs(hash(seed + 2)));
                    spike(ps, c, WHITE, o, len, hash(seed) * 180f, hash(seed + 1) * 90f);
                }
            }
        }
        float bl = beamLen(e.id(), t);
        if (bl > 0.05f && props.length > 0) {
            Prop p = props[0];
            double[] o = at(yawDeg, p.fwd(), p.right(), p.up());
            beam(ps, c, o, yawDeg, bl, 0.20f + 0.10f * sin(t * 30f),
                 e.id().equals("zoltraak") || e.id().equals("hollow_purple") ? PURPLE_CONC : SEA,
                 e.id().equals("zoltraak") || e.id().equals("hollow_purple") ? MAGENTA_GLASS : LB_GLASS);
        }
    }

    private static void cube(PoseStack ps, SubmitNodeCollector c, BlockState bs, double[] pos, float size, float yaw, float pitch) {
        ps.pushPose();
        ps.translate(pos[0], pos[1], pos[2]);
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
        ps.mulPose(Axis.XP.rotationDegrees(pitch));
        ps.scale(size, size, size);
        ps.translate(-0.5, -0.5, -0.5);
        c.submitBlock(ps, bs, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        ps.popPose();
    }

    /** A thin glowing needle pointing out of the orb in a random direction (a lightning arc). */
    private static void spike(PoseStack ps, SubmitNodeCollector c, BlockState bs, double[] pos, float len, float yaw, float pitch) {
        ps.pushPose();
        ps.translate(pos[0], pos[1], pos[2]);
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
        ps.mulPose(Axis.XP.rotationDegrees(pitch));
        ps.scale(0.03f, 0.03f, len);
        ps.translate(-0.5, -0.5, 0.0);
        c.submitBlock(ps, bs, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        ps.popPose();
    }

    /** A beam from the orb straight along the player's facing: a glass shell around a bright core. */
    private static void beam(PoseStack ps, SubmitNodeCollector c, double[] from, float yawDeg, float len, float width,
                             BlockState core, BlockState shell) {
        ps.pushPose();
        ps.translate(from[0], from[1], from[2]);
        ps.mulPose(Axis.YP.rotationDegrees(-yawDeg)); // makes local +Z point where the player faces
        ps.pushPose();
        ps.scale(width, width, len);
        ps.translate(-0.5, -0.5, 0.0);
        c.submitBlock(ps, shell, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        ps.popPose();
        ps.pushPose();
        ps.scale(width * 0.5f, width * 0.5f, len);
        ps.translate(-0.5, -0.5, 0.0);
        c.submitBlock(ps, core, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        ps.popPose();
        ps.popPose();
    }

    // ------------------------------------------------------------------ matching particles (sparks around the orbs)
    public static void sparks(ClientLevel level, Player p, Emote e, float t, boolean light, long gameTime) {
        Prop[] props = props(e.id(), t);
        if (props.length == 0) return;
        if (light && (gameTime + p.getId()) % 3 != 0) return;
        RandomSource r = level.random;
        ParticleOptions type = switch (e.id()) {
            case "chidori" -> ParticleTypes.ELECTRIC_SPARK;
            case "rasengan", "kamehameha", "spirit_gun" -> ParticleTypes.END_ROD;
            case "fireball_jutsu" -> ParticleTypes.FLAME;
            case "lapse_blue" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "reversal_red" -> ParticleTypes.CRIT;
            default -> ParticleTypes.WITCH;
        };
        int n = light ? 1 : 3;
        for (Prop pr : props) {
            if (pr.size() < 0.05f) continue;
            double[] o = at(p.yBodyRot, pr.fwd(), pr.right(), pr.up());
            for (int i = 0; i < n; i++) {
                double a = r.nextDouble() * TAU, rr = pr.size() * 0.7;
                level.addParticle(type, p.getX() + o[0] + Math.cos(a) * rr, p.getY() + o[1] + (r.nextDouble() - 0.5) * rr,
                    p.getZ() + o[2] + Math.sin(a) * rr, 0, 0.01, 0);
            }
        }
        float bl = beamLen(e.id(), t);
        if (bl > 0.2f && !light) { // a few motes drifting along the beam
            Prop pr = props[0];
            double[] o = at(p.yBodyRot, pr.fwd(), pr.right(), pr.up());
            double th = Math.toRadians(p.yBodyRot), fx = -Math.sin(th), fz = Math.cos(th);
            for (int i = 0; i < 3; i++) {
                double d = r.nextDouble() * bl;
                level.addParticle(ParticleTypes.END_ROD, p.getX() + o[0] + fx * d, p.getY() + o[1], p.getZ() + o[2] + fz * d, 0, 0, 0);
            }
        }
    }
}
