package com.example.emotewheel.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

/** The classic "poof!" cloud of smoke. */
public final class Smoke {
    private Smoke() {}

    public static void puff(ClientLevel level, double x, double y, double z, int count) {
        RandomSource r = level.random;
        for (int i = 0; i < count; i++) {
            double dx = (r.nextDouble() - 0.5) * 1.4, dy = r.nextDouble() * 1.9, dz = (r.nextDouble() - 0.5) * 1.4;
            level.addParticle(ParticleTypes.POOF, x + dx, y + dy, z + dz, dx * 0.05, 0.02 + r.nextDouble() * 0.04, dz * 0.05);
            if (i % 2 == 0) level.addParticle(ParticleTypes.CLOUD, x + dx, y + dy, z + dz, 0, 0.02, 0);
        }
    }
}
