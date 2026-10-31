package dev.skinoverride.net;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server side of cosmetic sharing: a tiny relay. Players with the mod tell the server their cape, wings and
 * Weapon Master loadout, and the server passes it to the players who can see them. The server never needs to
 * understand the text. Without this mod on the server everything still works, but only for you.
 */
public class SyncNet implements ModInitializer {

    /** Server thread only. */
    private static final Map<UUID, String> DATA = new HashMap<>();

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(CosmeticsUpPayload.ID, CosmeticsUpPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CosmeticsDownPayload.ID, CosmeticsDownPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(CosmeticsUpPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            String data = payload.data();
            if (data.length() > 400) {
                return;
            }
            if (data.isEmpty()) {
                DATA.remove(player.getUuid());
            } else {
                DATA.put(player.getUuid(), data);
            }
            CosmeticsDownPayload out = new CosmeticsDownPayload(player.getUuid().toString(), data);
            for (ServerPlayerEntity other : PlayerLookup.tracking(player)) {
                if (ServerPlayNetworking.canSend(other, CosmeticsDownPayload.ID)) {
                    ServerPlayNetworking.send(other, out);
                }
            }
        });

        EntityTrackingEvents.START_TRACKING.register((tracked, viewer) -> {
            if (tracked instanceof ServerPlayerEntity sp) {
                String data = DATA.get(sp.getUuid());
                if (data != null && ServerPlayNetworking.canSend(viewer, CosmeticsDownPayload.ID)) {
                    ServerPlayNetworking.send(viewer, new CosmeticsDownPayload(sp.getUuid().toString(), data));
                }
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                DATA.remove(handler.getPlayer().getUuid()));
    }
}
