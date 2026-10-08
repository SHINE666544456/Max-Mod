package com.example.emotewheel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.example.emotewheel.net.EmoteBroadcastPayload;
import com.example.emotewheel.net.PlayEmotePayload;
import com.example.emotewheel.net.StateBroadcastPayload;
import com.example.emotewheel.net.StatePayload;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Server side: a tiny relay. Clients tell the server which emote they started/stopped, and the server
 * forwards it to the players who can see them. The server never needs to know what an emote looks like.
 * Without this mod on the server, emotes still work, but only you can see them.
 */
public class EmoteWheelMod implements ModInitializer {
    public static final String MOD_ID = "emote_wheel";

    /** Server-thread only. */
    private static final Map<UUID, String> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> LAST_START = new HashMap<>();
    /** Per player: which persistent states (shadow clones, cat-girl) are currently on. */
    private static final Map<UUID, Set<String>> STATES = new HashMap<>();
    /** Locator-bar range a stealthed player had before we zeroed it, so it can be restored exactly. */
    private static final Map<UUID, Double> SAVED_RANGE = new HashMap<>();
    private static final long MIN_GAP_NANOS = 100_000_000L; // 0.1s between starts, to stop spam

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(PlayEmotePayload.TYPE, PlayEmotePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(EmoteBroadcastPayload.TYPE, EmoteBroadcastPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StatePayload.TYPE, StatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StateBroadcastPayload.TYPE, StateBroadcastPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(StatePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            String state = payload.state();
            if (!States.isKnown(state)) return;
            // turning a state OFF is always allowed; turning it ON needs permission
            if (payload.on() && !States.allowed(state, player.getName().getString())) return;

            Set<String> set = STATES.computeIfAbsent(player.getUUID(), k -> new HashSet<>());
            // Clone modes are mutually exclusive
            if (payload.on() && States.isCloneState(state)) {
                for (String other : States.CLONE_STATES) {
                    if (other.equals(state)) continue;
                    if (set.remove(other)) {
                        StateBroadcastPayload clear = new StateBroadcastPayload(player.getUUID(), other, false);
                        for (ServerPlayer otherP : PlayerLookup.tracking(player)) {
                            if (ServerPlayNetworking.canSend(otherP, StateBroadcastPayload.TYPE))
                                ServerPlayNetworking.send(otherP, clear);
                        }
                    }
                }
            }
            if (payload.on()) set.add(state); else set.remove(state);
            if (States.STEALTH.equals(state)) applyStealth(player, payload.on());

            StateBroadcastPayload out = new StateBroadcastPayload(player.getUUID(), state, payload.on());
            for (ServerPlayer other : PlayerLookup.tracking(player)) {
                if (ServerPlayNetworking.canSend(other, StateBroadcastPayload.TYPE)) ServerPlayNetworking.send(other, out);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(PlayEmotePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            String id = payload.emoteId();
            if (!isValidId(id)) return;

            UUID uuid = player.getUUID();
            if (id.isEmpty()) {
                ACTIVE.remove(uuid);
            } else {
                long now = System.nanoTime();
                Long last = LAST_START.get(uuid);
                if (last != null && now - last < MIN_GAP_NANOS) return;
                LAST_START.put(uuid, now);
                ACTIVE.put(uuid, id);
            }

            EmoteBroadcastPayload out = new EmoteBroadcastPayload(uuid, id);
            for (ServerPlayer other : PlayerLookup.tracking(player)) {
                send(other, out);
            }
        });

        // Someone just came into view: tell them what this player is doing right now (or that they're idle).
        EntityTrackingEvents.START_TRACKING.register((tracked, viewer) -> {
            if (tracked instanceof ServerPlayer sp) {
                send(viewer, new EmoteBroadcastPayload(sp.getUUID(), ACTIVE.getOrDefault(sp.getUUID(), "")));
                if (ServerPlayNetworking.canSend(viewer, StateBroadcastPayload.TYPE)) {
                    Set<String> on = STATES.getOrDefault(sp.getUUID(), Set.of());
                    for (String st : States.ALL) {
                        ServerPlayNetworking.send(viewer, new StateBroadcastPayload(sp.getUUID(), st, on.contains(st)));
                    }
                }
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUUID();
            ACTIVE.remove(uuid);
            LAST_START.remove(uuid);
            STATES.remove(uuid);
            SAVED_RANGE.remove(uuid);
        });

        // dying resets attributes, so put stealth back on the fresh player
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            Set<String> on = STATES.get(newPlayer.getUUID());
            if (on != null && on.contains(States.STEALTH)) {
                SAVED_RANGE.remove(newPlayer.getUUID());
                applyStealth(newPlayer, true);
            }
        });
    }

    /**
     * Stealth hides the player from everyone's locator bar by setting their waypoint transmit range to 0
     * (this works for vanilla clients too). Turning it off restores the exact previous value.
     */
    private static void applyStealth(ServerPlayer player, boolean on) {
        AttributeInstance range = player.getAttribute(Attributes.WAYPOINT_TRANSMIT_RANGE);
        if (range == null) return;
        if (on) {
            SAVED_RANGE.putIfAbsent(player.getUUID(), range.getBaseValue());
            range.setBaseValue(0.0);
        } else {
            Double old = SAVED_RANGE.remove(player.getUUID());
            if (old != null) range.setBaseValue(old);
        }
    }

    private static void send(ServerPlayer to, EmoteBroadcastPayload payload) {
        if (ServerPlayNetworking.canSend(to, EmoteBroadcastPayload.TYPE)) {
            ServerPlayNetworking.send(to, payload);
        }
    }

    /** Emote ids are lowercase letters, digits and underscores only. */
    private static boolean isValidId(String id) {
        if (id.length() > 40) return false;
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_')) return false;
        }
        return true;
    }
}
