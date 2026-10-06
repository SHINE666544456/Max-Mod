package com.example.emotewheel.client;

import com.example.emotewheel.emote.Emote;
import com.example.emotewheel.emote.Fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Spawns particles around anyone currently playing an emote.
 *
 * Particle setting (Options in the emote menu):
 *   Off   - nothing.
 *   Light - default. Only emotes with a signature effect, a short burst when they start and a slow trickle
 *           (every 8 ticks, for at most 4 seconds), only within 24 blocks.
 *   Full  - every emote that has any theme, bigger bursts, constant trickle, 48 blocks.
 */
public final class EmoteFx {
    private EmoteFx() {}

    private static final Item[] CONFETTI = {
        Items.RED_DYE, Items.ORANGE_DYE, Items.YELLOW_DYE, Items.LIME_DYE,
        Items.LIGHT_BLUE_DYE, Items.PINK_DYE, Items.MAGENTA_DYE, Items.PAPER, Items.GOLD_NUGGET
    };

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;
        EmoteConfig cfg = EmoteConfig.get();
        int mode = cfg.fxMode;
        if (mode <= 0) return;

        boolean light = mode == 1;
        double range = light ? 24 : 48;
        double maxDist2 = range * range;
        long gameTime = level.getGameTime();

        for (Player p : level.players()) {
            boolean self = p == mc.player;
            if (!self && !cfg.showOthers) continue;
            if (p.distanceToSqr(mc.player) > maxDist2) continue;
            Emote e = EmoteClient.current(p.getUUID());
            if (e == null) continue;
            if (EmoteProps.has(e)) { // the 3D props bring their own matching sparks
                EmoteProps.sparks(level, p, e, EmoteClient.time(p.getUUID()), light, gameTime);
                continue;
            }
            Fx fx = e.fxFor(mode);
            if (fx == Fx.NONE) continue;

            float t = EmoteClient.time(p.getUUID());
            boolean burst = t < (light ? 0.15f : 0.25f);
            if (light && !burst) {
                // trickle: one small puff every 8 ticks, and stop after 4 seconds so long loops don't spam
                if (t > 4f || (gameTime + p.getId()) % 8 != 0) continue;
            }
            spawn(level, p, fx, burst, light ? 0.35f : 1f);
        }
    }

    /** Scale a particle count (never below 1). */
    private static int c(float k, int base) { return Math.max(1, Math.round(base * k)); }

    private static void spawn(ClientLevel level, Player p, Fx fx, boolean burst, float k) {
        RandomSource r = level.random;
        double x = p.getX(), y = p.getY(), z = p.getZ();
        boolean full = k >= 1f;
        int n = burst ? c(k, 18) : (full ? 2 : 1);
        switch (fx) {
            case POPPER -> {
                if (burst) {
                    for (int i = 0; i < c(k, 22); i++) popper(level, r, x, y + 1.1, z, 0.55);
                    for (int i = 0; i < c(k, 14); i++) confetti(level, r, x, y + 1.3, z);
                } else if (r.nextInt(2) == 0) {
                    popper(level, r, x, y + 1.0 + r.nextDouble() * 0.8, z, 0.28);
                    if (r.nextInt(3) == 0) confetti(level, r, x, y + 1.4, z);
                }
            }
            case HEARTS -> ring(level, ParticleTypes.HEART, r, x, y + 1.4, z, burst ? c(k, 8) : 1, 0.4);
            case NOTES -> {
                for (int i = 0; i < (burst ? c(k, 6) : 1); i++) {
                    level.addParticle(ParticleTypes.NOTE, x + (r.nextDouble() - 0.5) * 0.8, y + 1.8 + r.nextDouble() * 0.3, z + (r.nextDouble() - 0.5) * 0.8, r.nextDouble(), 0, 0);
                }
            }
            case SPARKLE -> {
                ring(level, ParticleTypes.HAPPY_VILLAGER, r, x, y + 1.0, z, burst ? c(k, 8) : 1, 0.5);
                if (full && (burst || r.nextInt(3) == 0)) ring(level, ParticleTypes.END_ROD, r, x, y + 1.2, z, 2, 0.25);
            }
            case FLAME -> {
                for (int i = 0; i < n; i++) {
                    double dx = (r.nextDouble() - 0.5) * 0.7, dz = (r.nextDouble() - 0.5) * 0.7;
                    level.addParticle(ParticleTypes.FLAME, x + dx, y + 0.4 + r.nextDouble() * 1.4, z + dz, dx * 0.08, 0.04, dz * 0.08);
                    if (full && i % 2 == 0) level.addParticle(ParticleTypes.LAVA, x + dx, y + 0.8, z + dz, 0, 0.02, 0);
                }
            }
            case LIGHTNING -> {
                for (int i = 0; i < n; i++) {
                    double dx = (r.nextDouble() - 0.5) * 1.1, dy = r.nextDouble() * 1.8, dz = (r.nextDouble() - 0.5) * 1.1;
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, x + dx, y + dy, z + dz, dx, 0.05, dz);
                    if (full && burst) level.addParticle(ParticleTypes.FIREWORK, x + dx, y + dy, z + dz, dx * 0.4, 0.2, dz * 0.4);
                }
            }
            case WIND -> {
                for (int i = 0; i < n; i++) {
                    double dx = (r.nextDouble() - 0.5), dz = (r.nextDouble() - 0.5);
                    level.addParticle(ParticleTypes.CLOUD, x + dx * 0.4, y + 0.6 + r.nextDouble(), z + dz * 0.4, dx * 0.15, 0.02, dz * 0.15);
                    if (full && burst) level.addParticle(ParticleTypes.GUST, x, y + 0.4, z, 0, 0, 0);
                }
            }
            case AURA -> {
                ring(level, ParticleTypes.ENCHANT, r, x, y + 0.4, z, burst ? c(k, 14) : (full ? 3 : 1), 0.7);
                if (full) ring(level, ParticleTypes.GLOW, r, x, y + 1.0, z, burst ? 4 : 1, 0.35);
            }
            case PURPLE -> {
                for (int i = 0; i < n; i++) {
                    double dx = (r.nextDouble() - 0.5) * 0.9, dz = (r.nextDouble() - 0.5) * 0.9;
                    level.addParticle(ParticleTypes.WITCH, x + dx, y + 0.5 + r.nextDouble() * 1.4, z + dz, 0, 0.05, 0);
                    if (full) {
                        level.addParticle(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1f), x + dx, y + 1.0, z + dz, dx * 0.05, 0.02, dz * 0.05);
                        if (burst) level.addParticle(ParticleTypes.PORTAL, x + dx, y + 1.0, z + dz, dx, 0.2, dz);
                    }
                }
            }
            case BLUE -> {
                for (int i = 0; i < n; i++) {
                    double dx = (r.nextDouble() - 0.5) * 0.8, dz = (r.nextDouble() - 0.5) * 0.8;
                    level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x + dx, y + 0.6 + r.nextDouble(), z + dz, 0, 0.03, 0);
                    if (full) level.addParticle(ParticleTypes.SPLASH, x + dx, y + 1.2, z + dz, 0, 0.02, 0);
                }
            }
            case SOUL -> {
                ring(level, ParticleTypes.SOUL, r, x, y + 0.3, z, burst ? c(k, 8) : 1, 0.4);
                if (full && burst) ring(level, ParticleTypes.SCULK_SOUL, r, x, y + 1.0, z, 4, 0.3);
            }
            case RAIN -> {
                for (int i = 0; i < (burst ? c(k, 10) : (full ? 2 : 1)); i++) {
                    level.addParticle(ParticleTypes.FALLING_WATER, x + (r.nextDouble() - 0.5) * 0.6, y + 1.7, z + (r.nextDouble() - 0.5) * 0.6, 0, 0, 0);
                    if (full && r.nextBoolean()) level.addParticle(ParticleTypes.SPLASH, x + (r.nextDouble() - 0.5) * 0.5, y + 1.1, z + (r.nextDouble() - 0.5) * 0.5, 0, 0, 0);
                }
            }
            case SMOKE -> {
                if (burst) Smoke.puff(level, x, y, z, 20);
                else if (full) ring(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, r, x, y + 0.2, z, 1, 0.25);
            }
            case POWER -> {
                for (int i = 0; i < n; i++) {
                    double dx = (r.nextDouble() - 0.5), dz = (r.nextDouble() - 0.5);
                    level.addParticle(ParticleTypes.CRIT, x + dx * 0.5, y + 0.8 + r.nextDouble() * 0.8, z + dz * 0.5, dx, 0.1, dz);
                    if (full && (burst || r.nextInt(3) == 0)) level.addParticle(ParticleTypes.FLAME, x + dx * 0.3, y + 0.4, z + dz * 0.3, 0, 0.06, 0);
                }
            }
            case CHERRY -> ring(level, ParticleTypes.CHERRY_LEAVES, r, x, y + 1.6, z, burst ? c(k, 12) : (full ? 2 : 1), 0.7);
            case ANGRY -> {
                if (burst || (full && r.nextInt(4) == 0)) level.addParticle(ParticleTypes.ANGRY_VILLAGER, x, y + 2.1, z, 0, 0, 0);
                if (full) ring(level, ParticleTypes.SMOKE, r, x, y + 0.2, z, burst ? 6 : 1, 0.3);
            }
            default -> {}
        }
    }

    private static void popper(ClientLevel level, RandomSource r, double x, double y, double z, double speed) {
        double dx = (r.nextDouble() - 0.5) * 2 * speed;
        double dy = 0.15 + r.nextDouble() * speed;
        double dz = (r.nextDouble() - 0.5) * 2 * speed;
        level.addParticle(ParticleTypes.FIREWORK, x, y, z, dx, dy, dz);
        if (r.nextBoolean()) level.addParticle(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, dx * 0.6, dy, dz * 0.6);
    }

    private static void confetti(ClientLevel level, RandomSource r, double x, double y, double z) {
        Item dye = CONFETTI[r.nextInt(CONFETTI.length)];
        double dx = (r.nextDouble() - 0.5) * 0.45;
        double dy = 0.15 + r.nextDouble() * 0.35;
        double dz = (r.nextDouble() - 0.5) * 0.45;
        level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(dye)), x, y, z, dx, dy, dz);
    }

    private static void ring(ClientLevel level, ParticleOptions type, RandomSource r,
                             double x, double y, double z, int count, double spread) {
        for (int i = 0; i < count; i++) {
            double dx = (r.nextDouble() - 0.5) * 2 * spread;
            double dz = (r.nextDouble() - 0.5) * 2 * spread;
            level.addParticle(type, x + dx, y + r.nextDouble() * 0.4, z + dz, 0, 0.02 + r.nextDouble() * 0.04, 0);
        }
    }
}
