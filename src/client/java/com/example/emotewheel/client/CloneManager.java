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

/** Creates and removes shadow clones for every player whose "shadow_clones" state is on (and who is allowed to have them). */
public final class CloneManager {
    private CloneManager() {}

    // (right, forward) offsets in blocks, relative to where the owner is facing when the clones appear
    private static final double[][] FORMATION = { { -2.2, 0.8 }, { 2.2, 0.8 }, { -1.4, -1.9 }, { 1.4, -1.9 } };

    private static final Map<UUID, List<CloneEntity>> CLONES = new HashMap<>();
    private static int nextFakeId = -1000;

    /** UUID whose emote this player should copy: itself, or the owner if this is a clone. */
    public static UUID emoteSource(AbstractClientPlayer p) {
        return p instanceof CloneEntity c ? c.ownerUuid() : p.getUUID();
    }

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null) { CLONES.clear(); return; }

        Iterator<Map.Entry<UUID, List<CloneEntity>>> it = CLONES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, List<CloneEntity>> e = it.next();
            List<CloneEntity> list = e.getValue();
            AbstractClientPlayer owner = list.get(0).owner();
            boolean sameLevel = list.get(0).level() == level;
            boolean keep = sameLevel && !owner.isRemoved() && owner.isAlive() && wants(owner);
            if (!keep) {
                if (sameLevel) for (CloneEntity c : list) vanish(level, c);
                it.remove();
            }
        }

        for (AbstractClientPlayer p : level.players()) {
            if (CLONES.containsKey(p.getUUID()) || !wants(p) || !p.isAlive()) continue;
            CLONES.put(p.getUUID(), spawn(level, p));
        }
    }

    public static void clear() { CLONES.clear(); }

    private static boolean wants(AbstractClientPlayer p) {
        return ClientStates.has(p.getUUID(), States.SHADOW_CLONES)
            && States.allowed(States.SHADOW_CLONES, p.getName().getString());
    }

    private static List<CloneEntity> spawn(ClientLevel level, AbstractClientPlayer owner) {
        double yaw = Math.toRadians(owner.getYRot());
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);   // forward
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);  // right
        List<CloneEntity> list = new ArrayList<>();
        for (double[] f : FORMATION) {
            double ox = rx * f[0] + fx * f[1], oz = rz * f[0] + fz * f[1];
            CloneEntity c = new CloneEntity(level, owner, ox, oz);
            c.setId(nextFakeId--); // negative ids can never clash with real server entities
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
