package dev.skinoverride.net;

import dev.skinoverride.Cosmetics;
import dev.skinoverride.chat.ChatExtras;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import java.util.UUID;

/** Client side of cosmetic sharing: sends your look when it changes and stores what others send. */
public final class CosmeticSync {

    private static String lastSent;
    private static int timer;
    private static int resend;

    private CosmeticSync() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(CosmeticsDownPayload.ID, (payload, context) -> {
            try {
                Cosmetics.setRemote(UUID.fromString(payload.uuid()), payload.data());
            } catch (IllegalArgumentException e) {
                // ignore a malformed id
            }
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            lastSent = null;
            Cosmetics.clearRemotes();
            ChatExtras.clear();
            dev.skinoverride.VillagerTrades.clear();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            lastSent = null;
            Cosmetics.clearRemotes();
        });
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || ++timer < 10) {
            return;
        }
        timer = 0;
        if (!ClientPlayNetworking.canSend(CosmeticsUpPayload.ID)) {
            return;
        }
        String data = Cosmetics.localData(mc);
        // also resend now and then so players who walk into range later always get it
        boolean refresh = ++resend >= 60 && !data.isEmpty();
        if (!data.equals(lastSent) || refresh) {
            resend = 0;
            lastSent = data;
            ClientPlayNetworking.send(new CosmeticsUpPayload(data));
        }
    }
}
