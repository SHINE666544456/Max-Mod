package com.example.emotewheel.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.example.emotewheel.States;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Creates and removes shadow clones for every player whose clone state is on.
 * Count depends on which jutsu is active:
 *  - just_a_clone → 1
 *  - shadow_clones → 3
 *  - multi_shadow_clones → EmoteConfig.multiCloneCount (owner), or 6 for other players
 */
public final class CloneManager {
    private CloneManager() {}

    private static final Map<UUID, List<CloneEntity>> CLONES = new HashMap<>();
    /** Desired clone count when we last spawned for this owner (so we can respawn if it changes). */
    private static final Map<UUID, Integer> SPAWNED_COUNT = new HashMap<>();
    private static int nextFakeId = -1000;

    /** UUID whose emote this player should copy: itself, or the owner if this is a clone. */
    public static UUID emoteSource(AbstractClientPlayer p) {
        return p instanceof CloneEntity c ? c.ownerUuid() : p.getUUID();
    }

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null) { CLONES.clear(); SPAWNED_COUNT.clear(); return; }

        Iterator<Map.Entry<UUID, List<CloneEntity>>> it = CLONES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, List<CloneEntity>> e = it.next();
            List<CloneEntity> list = e.getValue();
            AbstractClientPlayer owner = list.get(0).owner();
            boolean sameLevel = list.get(0).level() == level;
            int desired = countFor(owner);
            boolean keep = sameLevel && !owner.isRemoved() && owner.isAlive() && desired > 0
                && SPAWNED_COUNT.getOrDefault(e.getKey(), -1) == desired;
            if (!keep) {
                if (sameLevel) for (CloneEntity c : list) vanish(level, c);
                it.remove();
                SPAWNED_COUNT.remove(e.getKey());
            }
        }

        // Copy the list: spawning a clone adds it to level.players() while we're still looping (that crashed the game).
        for (AbstractClientPlayer p : new ArrayList<>(level.players())) {
            if (p instanceof CloneEntity) continue;
            if (CLONES.containsKey(p.getUUID()) || !p.isAlive()) continue;
            int n = countFor(p);
            if (n <= 0) continue;
            CLONES.put(p.getUUID(), spawn(level, p, n));
            SPAWNED_COUNT.put(p.getUUID(), n);
        }
    }

    public static void clear() { CLONES.clear(); SPAWNED_COUNT.clear(); }

    private static int countFor(AbstractClientPlayer p) {
        UUID id = p.getUUID();
        if (ClientStates.has(id, States.JUST_A_CLONE)) return 1;
        if (ClientStates.has(id, States.MULTI_SHADOW_CLONES)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getUUID().equals(id)) {
                return Math.max(1, EmoteConfig.get().multiCloneCount);
            }
            return 6; // other players: fixed default (their config is local-only)
        }
        if (ClientStates.has(id, States.SHADOW_CLONES)) return 3;
        return 0;
    }

    /**
     * The group's centre is placed to a random side of you (never on you), and the clones are scattered around that centre.
     */
    /**
     * Scatter clones randomly near the owner — not in a neat ring, and not pushed farther away
     * just because there are more of them. Slight extra room at high counts so they don't stack.
     */
    private static List<CloneEntity> spawn(ClientLevel level, AbstractClientPlayer owner, int count) {
        java.util.concurrent.ThreadLocalRandom rnd = java.util.concurrent.ThreadLocalRandom.current();
        // Stay close to the player. Max radius grows only a little with count (log), never a huge ring.
        double minR = 1.15;
        double maxR = count <= 1 ? 2.2
            : count <= 3 ? 3.2
            : Math.min(5.0, 3.0 + Math.log(count) * 0.55);

        List<CloneEntity> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double ox = 0, oz = 0;
            // Rejection sample in a disc so placement is random, not evenly spaced on a circle
            for (int tries = 0; tries < 24; tries++) {
                double x = (rnd.nextDouble() * 2 - 1) * maxR;
                double z = (rnd.nextDouble() * 2 - 1) * maxR;
                double len = Math.hypot(x, z);
                if (len >= minR && len <= maxR) {
                    ox = x;
                    oz = z;
                    break;
                }
                if (tries == 23) {
                    // fallback: random angle at mid radius
                    double ang = rnd.nextDouble() * Math.PI * 2;
                    double rad = minR + rnd.nextDouble() * (maxR - minR);
                    ox = Math.cos(ang) * rad;
                    oz = Math.sin(ang) * rad;
                }
            }
            CloneEntity c = new CloneEntity(level, owner, ox, oz);
            c.setId(nextFakeId--);
            level.addEntity(c);
            Smoke.puff(level, c.getX(), c.getY(), c.getZ(), 22);
            list.add(c);
        }
        return list;
    }

    private static void vanish(ClientLevel level, CloneEntity c) {
        Smoke.puff(level, c.getX(), c.getY(), c.getZ(), 22);
        level.removeEntity(c.getId(), Entity.RemovalReason.DISCARDED);
    }
}
