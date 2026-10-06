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

        // Copy the list: spawning a clone adds it to level.players() while we're still looping (that crashed the game).
        for (AbstractClientPlayer p : new ArrayList<>(level.players())) {
            if (p instanceof CloneEntity) continue;
            if (CLONES.containsKey(p.getUUID()) || !wants(p) || !p.isAlive()) continue;
            CLONES.put(p.getUUID(), spawn(level, p));
        }
    }

    public static void clear() { CLONES.clear(); }

    private static boolean wants(AbstractClientPlayer p) {
        return ClientStates.has(p.getUUID(), States.SHADOW_CLONES)
            && States.allowed(States.SHADOW_CLONES, p.getName().getString());
    }

    /**
     * The group's centre is placed to a random side of you (never on you), and the clones are scattered round it,
     * so you are never reliably "the one in the middle". Number and layout are different every time.
     */
    private static List<CloneEntity> spawn(ClientLevel level, AbstractClientPlayer owner) {
        java.util.concurrent.ThreadLocalRandom rnd = java.util.concurrent.ThreadLocalRandom.current();
        int count = 4 + rnd.nextInt(3);                            // 4 to 6 clones
        double dir = rnd.nextDouble() * Math.PI * 2;                // which way the group sits relative to you
        double dist = 1.6 + rnd.nextDouble() * 1.6;
        double cx = Math.cos(dir) * dist, cz = Math.sin(dir) * dist;
        double offset = rnd.nextDouble() * Math.PI * 2;

        List<CloneEntity> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double ang = offset + i * (Math.PI * 2 / count) + (rnd.nextDouble() - 0.5) * 0.7;
            double rad = 1.3 + rnd.nextDouble() * 1.2;
            double ox = cx + Math.cos(ang) * rad, oz = cz + Math.sin(ang) * rad;
            double len = Math.hypot(ox, oz);
            if (len < 1.1) { ox *= 1.1 / Math.max(len, 0.01); oz *= 1.1 / Math.max(len, 0.01); } // don't stand on the real you
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
