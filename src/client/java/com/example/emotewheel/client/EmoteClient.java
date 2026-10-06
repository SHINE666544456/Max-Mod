package com.example.emotewheel.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.example.emotewheel.States;
import com.example.emotewheel.emote.Emote;
import com.example.emotewheel.emote.Emotes;
import com.example.emotewheel.net.PlayEmotePayload;
import com.example.emotewheel.net.StatePayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;

/** Everything about which emote each player is currently playing, on this client. */
public final class EmoteClient {
    private EmoteClient() {}

    private record Active(Emote emote, long startNanos) {}

    private static final Map<UUID, Active> ACTIVE = new HashMap<>();
    private static CameraType cameraBefore = null;
    private static int localTicks = 0;
    private static boolean localActive = false;

    // a state toggle (shadow clones / cat-girl) waiting for its moment in the animation
    private static String pendingState = null;
    private static int pendingTicks = 0;

    // live preview in the emote menu
    private static Emote previewEmote = null;
    private static long previewStart = 0;
    private static boolean previewing = false;

    /** The emote this player is playing right now, or null. Clears finished one-shot emotes. */
    public static Emote current(UUID id) {
        Active a = ACTIVE.get(id);
        if (a == null) return null;
        if (a.emote().isOneShot() && seconds(a) > a.emote().duration()) {
            ACTIVE.remove(id);
            return null;
        }
        return a.emote();
    }

    /** Seconds since that player's emote began. */
    public static float time(UUID id) {
        Active a = ACTIVE.get(id);
        return a == null ? 0f : seconds(a);
    }

    private static float seconds(Active a) {
        return (System.nanoTime() - a.startNanos()) / 1_000_000_000f;
    }

    // ---- preview (emote menu) ----

    public static void setPreview(Emote e) {
        if (e != previewEmote) { previewEmote = e; previewStart = System.nanoTime(); }
    }
    public static void beginPreview() { previewing = true; }
    public static void endPreview() { previewing = false; }
    public static boolean isPreviewing() { return previewing; }
    public static Emote previewEmote() { return previewEmote; }
    public static float previewTime() {
        float t = (System.nanoTime() - previewStart) / 1_000_000_000f;
        if (previewEmote != null && previewEmote.isOneShot()) t = t % (previewEmote.duration() + 0.6f); // loop one-shots
        return t;
    }

    // ---- local player ----

    /** Play an emote on yourself. Playing the one you're already doing stops it. */
    public static void playLocal(Emote emote) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || emote == null || !EmoteAccess.canUse(emote)) return;

        if (emote.state() == null && current(player.getUUID()) == emote) {
            stopLocal();
            return;
        }
        ACTIVE.put(player.getUUID(), new Active(emote, System.nanoTime()));
        EmoteConfig.get().markUsed(emote.id());
        localTicks = 0;
        localActive = true;
        send(emote.id());

        if (emote.state() != null) {
            pendingState = emote.state();
            pendingTicks = Math.max(1, emote.stateDelay());
        }

        EmoteConfig cfg = EmoteConfig.get();
        Options o = mc.options;
        if (cfg.thirdPerson && o.getCameraType().isFirstPerson()) {
            cameraBefore = o.getCameraType();
            o.setCameraType(CameraType.THIRD_PERSON_FRONT);
        }
    }

    public static void stopLocal() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        ACTIVE.remove(player.getUUID());
        if (localActive) { localActive = false; send(""); }
        if (cameraBefore != null) {
            mc.options.setCameraType(cameraBefore);
            cameraBefore = null;
        }
    }

    private static void send(String id) {
        if (ClientPlayNetworking.canSend(PlayEmotePayload.TYPE)) {
            ClientPlayNetworking.send(new PlayEmotePayload(id));
        }
    }

    private static void toggleState(LocalPlayer player, String state) {
        boolean on = !ClientStates.has(player.getUUID(), state);
        if (on && !States.allowed(state, player.getName().getString())) return;
        ClientStates.set(player.getUUID(), state, on);
        String label = switch (state) {
            case States.SHADOW_CLONES -> "Shadow clones";
            case States.CAT_GIRL -> "Transformation";
            case States.SUBSTITUTION -> "Substitution";
            case States.STEALTH -> "Stealth";
            default -> state;
        };
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(label + ": " + (on ? "ON" : "OFF")), true);
        if (ClientPlayNetworking.canSend(StatePayload.TYPE)) {
            ClientPlayNetworking.send(new StatePayload(state, on));
        }
    }

    // ---- other players ----

    public static void onRemote(UUID player, String id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getUUID().equals(player)) return; // we drive our own state
        Emote e = Emotes.byId(id);
        if (e == null) ACTIVE.remove(player);
        else ACTIVE.put(player, new Active(e, System.nanoTime()));
    }

    public static void clear() {
        ACTIVE.clear();
        cameraBefore = null;
        localActive = false;
        pendingState = null;
    }

    // ---- per-tick ----

    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (pendingState != null && --pendingTicks <= 0) {
            String st = pendingState;
            pendingState = null;
            toggleState(player, st);
        }

        Emote cur = current(player.getUUID());
        if (cur == null) {
            // a one-shot emote ran out by itself: tidy up and tell the server
            if (localActive) {
                localActive = false;
                send("");
                if (cameraBefore != null) {
                    mc.options.setCameraType(cameraBefore);
                    cameraBefore = null;
                }
            }
            return;
        }
        localTicks++;
        if (!player.isAlive() || (player.hurtTime > 0 && !cur.allowsMovement())) {
            stopLocal();
            return;
        }
        // Walk/Move emotes keep playing while you move; everything else stops when you do.
        if (EmoteConfig.get().cancelOnMove && !cur.allowsMovement() && localTicks > 3 && mc.screen == null) {
            Options o = mc.options;
            if (o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown() || o.keyRight.isDown()
                || o.keyJump.isDown() || o.keyShift.isDown() || o.keyAttack.isDown()) {
                stopLocal();
            }
        }
    }
}
