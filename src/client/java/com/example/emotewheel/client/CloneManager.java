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
    private static List<CloneEntity> spawn(ClientLevel level, AbstractClientPlayer owner, int count) {
        java.util.concurrent.ThreadLocalRandom rnd = java.util.concurrent.ThreadLocalRandom.current();

        // Spread scales with count so multi-shadow armies fan out instead of stacking on you.
        // ~2.5 blocks for a few clones; grows with sqrt(count) so dozens fill a wider ring.
        double spread = count <= 1 ? 1.5
            : count <= 3 ? 2.5
            : 2.0 + Math.sqrt(count) * 1.15;          // e.g. 8→~5.3, 20→~7.1, 50→~10.1
        double ringInner = Math.max(1.4, spread * 0.45);
        double ringOuter = spread;

        List<CloneEntity> list = new ArrayList<>();
        double offset = rnd.nextDouble() * Math.PI * 2;
        for (int i = 0; i < count; i++) {
            double ox, oz;
            if (count == 1) {
                double dir = rnd.nextDouble() * Math.PI * 2;
                double d = 1.4 + rnd.nextDouble() * 0.8;
                ox = Math.cos(dir) * d;
                oz = Math.sin(dir) * d;
            } else {
                // Evenly around a ring, with jitter; larger counts use a thicker annulus
                double ang = offset + i * (Math.PI * 2 / count) + (rnd.nextDouble() - 0.5) * (0.35 + 0.15 / Math.sqrt(count));
                double t = count <= 4 ? 0.5 : rnd.nextDouble(); // fill the ring depth for big groups
                double rad = ringInner + (ringOuter - ringInner) * t;
                rad += (rnd.nextDouble() - 0.5) * 0.6;
                ox = Math.cos(ang) * rad;
                oz = Math.sin(ang) * rad;
            }
            double len = Math.hypot(ox, oz);
            if (len < 1.2) {
                double s = 1.2 / Math.max(len, 0.01);
                ox *= s;
                oz *= s;
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
