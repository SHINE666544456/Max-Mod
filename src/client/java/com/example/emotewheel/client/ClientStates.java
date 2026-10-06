package com.example.emotewheel.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.example.emotewheel.States;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;

/** Which persistent states (shadow clones, cat-girl transformation) are on for which players, as this client knows it. */
public final class ClientStates {
    private ClientStates() {}

    private static final Map<UUID, Set<String>> MAP = new HashMap<>();

    public static boolean has(UUID id, String state) {
        Set<String> s = MAP.get(id);
        return s != null && s.contains(state);
    }

    /** Returns true if the value actually changed. Transformations get a puff of smoke. */
    public static boolean set(UUID id, String state, boolean on) {
        Set<String> s = MAP.computeIfAbsent(id, k -> new HashSet<>());
        boolean changed = on ? s.add(state) : s.remove(state);
        if (changed && (States.CAT_GIRL.equals(state) || States.SUBSTITUTION.equals(state))) {
            ClientLevel level = Minecraft.getInstance().level;
            Player p = level == null ? null : level.getPlayerByUUID(id);
            if (p != null) Smoke.puff(level, p.getX(), p.getY(), p.getZ(), 28);
        }
        return changed;
    }

    public static void clear() { MAP.clear(); }
}
