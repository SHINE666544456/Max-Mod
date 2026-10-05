package com.example.emotewheel.client;

import com.example.emotewheel.emote.Emote;
import com.example.emotewheel.emote.Fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Spawns particles around anyone currently playing an emote. */
public final class EmoteFx {
    private EmoteFx() {}

    private static final Item[] CONFETTI = {
        Items.RED_DYE, Items.ORANGE_DYE, Items.YELLOW_DYE, Items.LIME_DYE,
        Items.LIGHT_BLUE_DYE, Items.PINK_DYE, Items.MAGENTA_DYE, Items.PAPER, Items.GOLD_NUGGET
    };

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;
        double maxDist2 = 48 * 48;
        for (Player p : level.players()) {
            if (p.distanceToSqr(mc.player) > maxDist2) continue;
            Emote e = EmoteClient.current(p.getUUID());
            if (e == null || e.fx() == Fx.NONE) continue;
            float t = EmoteClient.time(p.getUUID());
            spawn(level, p, e.fx(), t);
        }
    }

    private static void spawn(ClientLevel level, Player p, Fx fx, float t) {
        RandomSource r = level.random;
        double x = p.getX(), y = p.getY(), z = p.getZ();
        boolean burst = t < 0.25f;
        int n = burst ? 18 : 2;
        switch (fx) {
            case POPPER -> {
                if (burst) {
                    for (int i = 0; i < 22; i++) popper(level, r, x, y + 1.1, z, 0.55);
                    for (int i = 0; i < 14; i++) confetti(level, r, x, y + 1.3, z);
                } else if (r.nextInt(2) == 0) {
                    popper(level, r, x, y + 1.0 + r.nextDouble() * 0.8, z, 0.28);
                    if (r.nextInt(3) == 0) confetti(level, r, x, y + 1.4, z);
                }
            }
            case HEARTS -> ring(level, ParticleTypes.HEART, r, x, y + 1.4, z, burst ? 8 : 1, 0.4);
            case NOTES -> {
                for (int i = 0; i < (burst ? 6 : 1); i++) {
                    level.addParticle(ParticleTypes.NOTE, x + (r.nextDouble() - 0.5) * 0.8, y + 1.8 + r.nextDouble() * 0.3, z + (r.nextDouble() - 0.5) * 0.8, r.nextDouble(), 0, 0);
                }
            }
            case SPARKLE -> {
                ring(level, ParticleTypes.HAPPY_VILLAGER, r, x, y + 1.0, z, burst ? 8 : 1, 0.5);
                if (burst || r.nextInt(3) == 0) ring(level, ParticleTypes.END_ROD, r, x, y + 1.2, z, 2, 0.25);
            }
            case FLAME -> {
                for (int i = 0; i < n; i++) {
                    double dx = (r.nextDouble() - 0.5) * 0.7, dz = (r.nextDouble() - 0.5) * 0.7;
                    level.addParticle(ParticleTypes.FLAME, x + dx, y + 0.4 + r.nextDouble() * 1.4, z + dz, dx * 0.08, 0.04, dz * 0.08);
                    if (i % 2 == 0) level.addParticle(ParticleTypes.LAVA, x + dx, y + 0.8, z + dz, 0, 0.02, 0);
                }
            }
            case LIGHTNING -> {
                for (int i = 0; i < n; i++) {
                    double dx = (r.nextDouble() - 0.5) * 1.1, dy = r.nextDouble() * 1.8, dz = (r.nextDouble() - 0.5) * 1.1;
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, x + dx, y + dy, z + dz, dx, 0.05, dz);
                    if (burst) level.addParticle(ParticleTypes.FIREWORK, x + dx, y + dy, z + dz, dx * 0.4, 0.2, dz * 0.4);
                }
            }
            case WIND -> {
                for (int i = 0; i < n; i++) {
                    double dx = (r.nextDouble() - 0.5), dz = (r.nextDouble() - 0.5);
                    level.addParticle(ParticleTypes.CLOUD, x + dx * 0.4, y + 0.6 + r.nextDouble(), z + dz * 0.4, dx * 0.15, 0.02, dz * 0.15);
                    if (burst) level.addParticle(ParticleTypes.GUST, x, y + 0.4, z, 0, 0, 0);
                }
            }
            case AURA -> {
                ring(level, ParticleTypes.ENCHANT, r, x, y + 0.4, z, burst ? 14 : 3, 0.7);
                ring(level, ParticleTypes.GLOW, r, x, y + 1.0, z, burst ? 4 : 1, 0.35);
            }
            case PURPLE -> {
                for (int i = 0; i < n; i++) {
                    double dx = (r.nextDouble() - 0.5) * 0.9, dz = (r.nextDouble() - 0.5) * 0.9;
                    level.addParticle(ParticleTypes.WITCH, x + dx, y + 0.5 + r.nextDouble() * 1.4, z + dz, 0, 0.05, 0);
                    level.addParticle(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1f), x + dx, y + 1.0, z + dz, dx * 0.05, 0.02, dz * 0.05);
                    if (burst) level.addParticle(ParticleTypes.PORTAL, x + dx, y + 1.0, z + dz, dx, 0.2, dz);
                }
            }
            case BLUE -> {
                for (int i = 0; i < n; i++) {
                    double dx = (r.nextDouble() - 0.5) * 0.8, dz = (r.nextDouble() - 0.5) * 0.8;
                    level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x + dx, y + 0.6 + r.nextDouble(), z + dz, 0, 0.03, 0);
                    level.addParticle(ParticleTypes.SPLASH, x + dx, y + 1.2, z + dz, 0, 0.02, 0);
                }
            }
            case SOUL -> {
                ring(level, ParticleTypes.SOUL, r, x, y + 0.3, z, burst ? 8 : 1, 0.4);
                if (burst) ring(level, ParticleTypes.SCULK_SOUL, r, x, y + 1.0, z, 4, 0.3);
            }
            case RAIN -> {
                for (int i = 0; i < (burst ? 10 : 2); i++) {
                    level.addParticle(ParticleTypes.FALLING_WATER, x + (r.nextDouble() - 0.5) * 0.6, y + 1.7, z + (r.nextDouble() - 0.5) * 0.6, 0, 0, 0);
                    if (r.nextBoolean()) level.addParticle(ParticleTypes.SPLASH, x + (r.nextDouble() - 0.5) * 0.5, y + 1.1, z + (r.nextDouble() - 0.5) * 0.5, 0, 0, 0);
                }
            }
            case SMOKE -> {
                if (burst) Smoke.puff(level, x, y, z, 20);
                else ring(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, r, x, y + 0.2, z, 1, 0.25);
            }
            case POWER -> {
                for (int i = 0; i < n; i++) {
                    double dx = (r.nextDouble() - 0.5), dz = (r.nextDouble() - 0.5);
                    level.addParticle(ParticleTypes.CRIT, x + dx * 0.5, y + 0.8 + r.nextDouble() * 0.8, z + dz * 0.5, dx, 0.1, dz);
                    if (burst || r.nextInt(3) == 0) level.addParticle(ParticleTypes.FLAME, x + dx * 0.3, y + 0.4, z + dz * 0.3, 0, 0.06, 0);
                }
            }
            case CHERRY -> ring(level, ParticleTypes.CHERRY_LEAVES, r, x, y + 1.6, z, burst ? 12 : 2, 0.7);
            case ANGRY -> {
                if (burst || r.nextInt(4) == 0) {
                    level.addParticle(ParticleTypes.ANGRY_VILLAGER, x, y + 2.1, z, 0, 0, 0);
                }
                ring(level, ParticleTypes.SMOKE, r, x, y + 0.2, z, burst ? 6 : 1, 0.3);
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

    private static void ring(ClientLevel level, net.minecraft.core.particles.ParticleOptions type, RandomSource r,
                             double x, double y, double z, int count, double spread) {
        for (int i = 0; i < count; i++) {
            double dx = (r.nextDouble() - 0.5) * 2 * spread;
            double dz = (r.nextDouble() - 0.5) * 2 * spread;
            level.addParticle(type, x + dx, y + r.nextDouble() * 0.4, z + dz, 0, 0.02 + r.nextDouble() * 0.04, 0);
        }
    }
}
