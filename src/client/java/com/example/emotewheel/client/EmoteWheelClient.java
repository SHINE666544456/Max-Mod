package com.example.emotewheel.client;

import com.example.emotewheel.client.screen.EmoteMenuScreen;
import com.example.emotewheel.client.screen.EmoteWheelScreen;
import com.example.emotewheel.emote.Emote;
import com.example.emotewheel.emote.Emotes;
import com.example.emotewheel.net.EmoteBroadcastPayload;
import com.example.emotewheel.net.StateBroadcastPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public class EmoteWheelClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EmoteKeys.register();
        EmoteConfig.get();
        LookTextures.init();

        ClientPlayNetworking.registerGlobalReceiver(EmoteBroadcastPayload.TYPE, (payload, context) ->
            context.client().execute(() -> EmoteClient.onRemote(payload.player(), payload.emoteId())));

        ClientPlayNetworking.registerGlobalReceiver(StateBroadcastPayload.TYPE, (payload, context) ->
            context.client().execute(() -> {
                // the local player's own states are driven locally; ignore the echo
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.player.getUUID().equals(payload.player())) return;
                ClientStates.set(payload.player(), payload.state(), payload.on());
            }));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            EmoteClient.clear();
            ClientStates.clear();
            CloneManager.clear();
        });

        ClientTickEvents.END_CLIENT_TICK.register(EmoteWheelClient::tick);
    }

    private static void tick(Minecraft mc) {
        EmoteClient.tick(mc);
        EmoteFx.tick(mc);
        CloneManager.tick(mc);
        if (mc.player == null) return;

        while (EmoteKeys.WHEEL.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new EmoteWheelScreen());
        }
        while (EmoteKeys.MENU.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new EmoteMenuScreen(null, -1));
        }
        while (EmoteKeys.STOP.consumeClick()) {
            EmoteClient.stopLocal();
        }
        EmoteConfig cfg = EmoteConfig.get();
        while (EmoteKeys.REPEAT.consumeClick()) {
            if (mc.screen != null) continue;
            Emote last = Emotes.byId(cfg.lastEmote);
            if (last != null) EmoteClient.playLocal(last); // playing the one you're already doing stops it
        }
        for (int i = 0; i < EmoteKeys.QUICK.length; i++) {
            while (EmoteKeys.QUICK[i].consumeClick()) {
                if (mc.screen != null) continue;
                Emote e = Emotes.byId(cfg.slotId(cfg.page, i));
                if (e != null) EmoteClient.playLocal(e);
            }
        }
    }
}
