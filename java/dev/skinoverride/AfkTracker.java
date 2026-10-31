package dev.skinoverride;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Guesses who is AFK by watching other players your client can see: if a player has not moved,
 * turned their head or toggled sneaking for the configured time, they are marked AFK.
 *
 * Limits: it can only watch players loaded near you (render distance), a player starts
 * counting from the moment you first see them, and being pushed (water, pistons) counts as activity.
 */
public final class AfkTracker {

    private static final double MOVE_EPSILON_SQ = 0.0009; // ~0.03 blocks
    private static final float TURN_EPSILON = 1.5f;

    private static final class State {
        double x, y, z;
        float yaw, pitch;
        boolean sneaking;
        long lastActiveTick;
    }

    private static final Map<UUID, State> states = new HashMap<>();
    private static ClientWorld lastWorld;
    private static long tickCounter;

    private AfkTracker() {
    }

    public static void tick(MinecraftClient mc) {
        tickCounter++;
        if (mc.world != lastWorld) {
            states.clear();
            lastWorld = mc.world;
        }
        if (mc.world == null || !ModConfig.get().afkTracking) {
            return;
        }
        Set<UUID> seen = new HashSet<>();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) {
                continue;
            }
            UUID id = p.getUuid();
            seen.add(id);
            State s = states.get(id);
            if (s == null) {
                s = new State();
                s.x = p.getX();
                s.y = p.getY();
                s.z = p.getZ();
                s.yaw = p.getYaw();
                s.pitch = p.getPitch();
                s.sneaking = p.isSneaking();
                s.lastActiveTick = tickCounter;
                states.put(id, s);
                continue;
            }
            double dx = p.getX() - s.x;
            double dy = p.getY() - s.y;
            double dz = p.getZ() - s.z;
            boolean moved = dx * dx + dy * dy + dz * dz > MOVE_EPSILON_SQ;
            boolean turned = Math.abs(p.getYaw() - s.yaw) > TURN_EPSILON
                    || Math.abs(p.getPitch() - s.pitch) > TURN_EPSILON;
            boolean sneakChanged = p.isSneaking() != s.sneaking;
            if (moved || turned || sneakChanged) {
                s.lastActiveTick = tickCounter;
                s.x = p.getX();
                s.y = p.getY();
                s.z = p.getZ();
                s.yaw = p.getYaw();
                s.pitch = p.getPitch();
                s.sneaking = p.isSneaking();
            }
        }
        states.keySet().retainAll(seen);
    }

    /** Seconds this player has been still, or -1 if unknown (not nearby / tracking off). */
    public static int idleSeconds(UUID id) {
        if (!ModConfig.get().afkTracking) {
            return -1;
        }
        State s = states.get(id);
        return s == null ? -1 : (int) ((tickCounter - s.lastActiveTick) / 20);
    }

    public static boolean isAfk(UUID id) {
        int idle = idleSeconds(id);
        return idle >= 0 && idle >= ModConfig.get().afkSeconds;
    }
}
