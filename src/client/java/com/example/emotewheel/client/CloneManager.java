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

        // Real player punches a clone → that clone poofs (all jutsu types).
        handlePunch(mc, level);

        Iterator<Map.Entry<UUID, List<CloneEntity>>> it = CLONES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, List<CloneEntity>> e = it.next();
            List<CloneEntity> list = e.getValue();
            AbstractClientPlayer owner = list.get(0).owner();
            boolean sameLevel = list.get(0).level() == level;
            int desired = countFor(owner);
            // Stay up while the jutsu is on. If the configured count changed, respawn the pack.
            // Punching clones only removes those entities; it must NOT force a full respawn.
            boolean stateOn = desired > 0;
            boolean countChanged = SPAWNED_COUNT.getOrDefault(e.getKey(), -1) != desired;
            boolean keep = sameLevel && !owner.isRemoved() && owner.isAlive() && stateOn && !countChanged;
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
    /**
     * Random scatter in a tight cluster around the owner.
     * - No empty ring around the player (can spawn right next to them).
     * - Soft minimum spacing between clones so they don't fully overlap.
     * - Not a neat circle and not pushed far away as count grows.
     */
    private static List<CloneEntity> spawn(ClientLevel level, AbstractClientPlayer owner, int count) {
        java.util.concurrent.ThreadLocalRandom rnd = java.util.concurrent.ThreadLocalRandom.current();
        // Compact area: grows gently so packs stay near you and hard to tell who is real
        double areaR = count <= 1 ? 1.6
            : count <= 4 ? 2.4
            : Math.min(3.8, 2.0 + Math.sqrt(count) * 0.35);
        double minSep = 0.72; // ~player width-ish; close but not overlapping

        List<double[]> placed = new ArrayList<>();
        List<CloneEntity> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double ox = 0, oz = 0;
            boolean ok = false;
            for (int tries = 0; tries < 40; tries++) {
                double x = (rnd.nextDouble() * 2 - 1) * areaR;
                double z = (rnd.nextDouble() * 2 - 1) * areaR;
                if (x * x + z * z > areaR * areaR) continue; // stay in disc
                boolean farEnough = true;
                for (double[] p : placed) {
                    double dx = x - p[0], dz = z - p[1];
                    if (dx * dx + dz * dz < minSep * minSep) { farEnough = false; break; }
                }
                if (farEnough) {
                    ox = x; oz = z;
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                // relax spacing: just random in disc
                double ang = rnd.nextDouble() * Math.PI * 2;
                double rad = rnd.nextDouble() * areaR;
                ox = Math.cos(ang) * rad;
                oz = Math.sin(ang) * rad;
            }
            placed.add(new double[] { ox, oz });
            CloneEntity c = new CloneEntity(level, owner, ox, oz);
            c.setId(nextFakeId--);
            level.addEntity(c);
            Smoke.puff(level, c.getX(), c.getY(), c.getZ(), 22);
            list.add(c);
        }
        return list;
    }

    private static boolean attackWasDown;

    /** Rising edge of attack: if a clone is under the crosshair (or right in front), poof it. */
    private static void handlePunch(Minecraft mc, ClientLevel level) {
        boolean down = mc.options != null && mc.options.keyAttack.isDown();
        boolean pressed = down && !attackWasDown;
        attackWasDown = down;
        if (!pressed || mc.player == null) return;

        CloneEntity target = null;
        if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult ehr
            && ehr.getEntity() instanceof CloneEntity c) {
            target = c;
        }
        if (target == null) {
            var look = mc.player.getLookAngle();
            double best = 3.0;
            for (List<CloneEntity> group : CLONES.values()) {
                for (CloneEntity c : group) {
                    if (c.isRemoved()) continue;
                    double dx = c.getX() - mc.player.getX();
                    double dy = c.getY() + c.getBbHeight() * 0.5 - mc.player.getEyeY();
                    double dz = c.getZ() - mc.player.getZ();
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (dist > 3.0 || dist < 0.01) continue;
                    double dot = (dx * look.x + dy * look.y + dz * look.z) / dist;
                    if (dot > 0.82 && dist < best) {
                        best = dist;
                        target = c;
                    }
                }
            }
        }
        if (target != null) destroyOne(level, target);
    }

    public static void destroyOne(ClientLevel level, CloneEntity c) {
        // Only remove this clone. Leave SPAWNED_COUNT alone so we don't respawn the pack.
        for (Map.Entry<UUID, List<CloneEntity>> e : new ArrayList<>(CLONES.entrySet())) {
            if (e.getValue().remove(c)) {
                if (e.getValue().isEmpty()) {
                    CLONES.remove(e.getKey());
                    SPAWNED_COUNT.remove(e.getKey());
                }
                break;
            }
        }
        vanish(level, c);
    }

    private static void vanish(ClientLevel level, CloneEntity c) {
        Smoke.puff(level, c.getX(), c.getY(), c.getZ(), 28);
        if (!c.isRemoved()) {
            level.removeEntity(c.getId(), Entity.RemovalReason.DISCARDED);
        }
    }
}
