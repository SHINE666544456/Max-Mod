package dev.skinoverride;

import dev.skinoverride.chat.ChatColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Floating damage numbers. A hit is detected when a nearby creature's health drops. "Only my hits"
 * counts drops on a creature you were attacking at that moment (you pressed attack while aiming at it).
 */
public final class DamageNumbers {

    private record Pop(double x, double y, double z, double dx, double dz,
                       String text, float amount, boolean heal, long born, long lifeMs) {
    }

    private static final Map<Integer, Float> LAST = new HashMap<>();
    private static final Map<Integer, Long> MARKS = new HashMap<>();
    private static final List<Pop> POPS = new ArrayList<>();
    private static final Random RNG = new Random();

    /** Time of your last detected hit; the crosshair hit marker reads this. */
    public static volatile long lastHitMs;

    private DamageNumbers() {
    }

    public static void tick(MinecraftClient mc) {
        ModConfig cfg = ModConfig.get();
        if (mc.world == null || mc.player == null) {
            LAST.clear();
            MARKS.clear();
            POPS.clear();
            return;
        }
        if (!cfg.dmgNumbers && !(cfg.crosshairHitMarker)) {
            LAST.clear();
            return;
        }
        long now = System.currentTimeMillis();

        // remember what you are attacking
        if (!Freecam.active() && mc.currentScreen == null
                && mc.targetedEntity instanceof LivingEntity target
                && PhysicalKeys.isDown(mc, mc.options.attackKey)) {
            MARKS.put(target.getId(), now);
        }
        MARKS.values().removeIf(t -> now - t > 3000L);

        Set<Integer> seen = new HashSet<>();
        double maxSq = 48.0 * 48.0;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity living) || e == mc.player) {
                continue;
            }
            if (mc.player.squaredDistanceTo(e) > maxSq) {
                continue;
            }
            int id = e.getId();
            seen.add(id);
            float effective = living.getHealth() + living.getAbsorptionAmount();
            Float prev = LAST.put(id, effective);
            if (prev == null) {
                continue;
            }
            float delta = prev - effective;
            boolean mine = now - MARKS.getOrDefault(id, 0L) < 1200L;
            if (delta > 0.05f) {
                if (mine) {
                    lastHitMs = now;
                }
                if (cfg.dmgNumbers && (!cfg.dmgOnlyMine || mine)) {
                    spawn(e, delta, false, now, cfg);
                }
            } else if (delta < -0.05f && cfg.dmgNumbers && cfg.dmgShowHeal && !cfg.dmgOnlyMine) {
                spawn(e, -delta, true, now, cfg);
            }
        }
        LAST.keySet().retainAll(seen);
        long cutoff = now;
        POPS.removeIf(p -> cutoff - p.born() > p.lifeMs());
    }

    private static void spawn(Entity e, float amount, boolean heal, long now, ModConfig cfg) {
        String number;
        if (cfg.dmgDecimals) {
            number = Math.abs(amount - Math.round(amount)) < 0.05f
                    ? String.valueOf(Math.round(amount))
                    : String.format(Locale.ROOT, "%.1f", amount);
        } else {
            number = String.valueOf(Math.max(1, Math.round(amount)));
        }
        POPS.add(new Pop(e.getX(), e.getY() + e.getHeight() + 0.25, e.getZ(),
                (RNG.nextDouble() - 0.5) * 0.8, (RNG.nextDouble() - 0.5) * 0.8,
                (heal ? "+" : "") + number, amount, heal, now, cfg.dmgDuration * 100L));
        while (POPS.size() > 40) {
            POPS.remove(0);
        }
    }

    private static int color(Pop p, long now, ModConfig cfg) {
        int mode = cfg.dmgColor;
        if (mode == ChatColors.RAINBOW) {
            return ChatColors.hsv((now % 1200L) / 1200f + (float) p.x() * 0.1f, 0.65f, 1f);
        }
        if (mode == ChatColors.CYCLE) {
            return ChatColors.cycleColor();
        }
        if (mode != ChatColors.DEFAULT) {
            return mode;
        }
        if (p.heal()) {
            return 0x55FF55;
        }
        if (p.amount() >= 10f) {
            return 0xFF5555;
        }
        if (p.amount() >= 6f) {
            return 0xFFAA00;
        }
        if (p.amount() >= 3f) {
            return 0xFFFF55;
        }
        return 0xFFFFFF;
    }

    public static void render(DrawContext c, MinecraftClient mc) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.dmgNumbers || POPS.isEmpty()) {
            return;
        }
        Projection proj = Projection.create(mc, mc.getRenderTickCounter().getTickProgress(false));
        if (proj == null) {
            return;
        }
        TextRenderer tr = mc.textRenderer;
        Matrix3x2fStack m = c.getMatrices();
        long now = System.currentTimeMillis();
        for (Pop p : POPS) {
            double frac = (now - p.born()) / (double) p.lifeMs();
            if (frac < 0 || frac > 1) {
                continue;
            }
            double rise = 1.1 * (1.0 - Math.pow(1.0 - frac, 2.0)); // fast start, eases out
            double[] s = proj.project(new Vec3d(p.x() + p.dx() * frac, p.y() + rise, p.z() + p.dz() * frac));
            if (s == null) {
                continue;
            }
            float distScale = (float) Math.max(0.6, Math.min(1.5, 12.0 / s[2]));
            float size = (cfg.dmgSize / 10f) * (0.9f + Math.min(p.amount(), 20f) / 40f) * distScale;
            int alpha = (int) (255 * (frac < 0.6 ? 1.0 : 1.0 - (frac - 0.6) / 0.4));
            int argb = Ui.withAlpha(color(p, now, cfg), Math.max(alpha, 12));

            m.pushMatrix();
            m.translate((float) s[0], (float) s[1]);
            m.scale(size, size);
            c.drawTextWithShadow(tr, p.text(), -tr.getWidth(p.text()) / 2, -4, argb);
            m.popMatrix();
        }
    }
}
