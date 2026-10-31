package dev.skinoverride;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import dev.skinoverride.chat.ChatProcessor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Watches the player list and shows a small pop-up when someone joins or leaves. */
public final class Notifications {

    public static final int LIFETIME_MS = 5000;
    private static final int MAX_ACTIVE = 5;

    public record Note(String name, String action, int color, long timeMs) {
    }

    private static final List<Note> ACTIVE = new ArrayList<>();
    private static Set<String> lastNames;
    private static long firstSeenMs;
    private static int timer;

    private Notifications() {
    }

    public static void tick(MinecraftClient mc) {
        ModConfig cfg = ModConfig.get();
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (mc.world == null || mc.player == null || handler == null) {
            lastNames = null;
            return;
        }
        if (++timer < 10) {
            return;
        }
        timer = 0;

        Set<String> now = new HashSet<>();
        for (PlayerListEntry entry : handler.getPlayerList()) {
            now.add(entry.getProfile().name());
        }
        long ms = System.currentTimeMillis();
        if (lastNames == null) {
            lastNames = now;
            firstSeenMs = ms;
            return;
        }
        // The list fills up when you connect: ignore the first few seconds.
        if (ms - firstSeenMs < 4000L || !cfg.notifyPopups) {
            lastNames = now;
            return;
        }
        String self = mc.player.getName().getString();
        for (String name : now) {
            if (!lastNames.contains(name) && cfg.notifyJoin && allowed(name, self, cfg)) {
                push(mc, name, "joined", Ui.GOOD, cfg);
            }
        }
        for (String name : lastNames) {
            if (!now.contains(name) && cfg.notifyLeave && allowed(name, self, cfg)) {
                push(mc, name, "left", Ui.BAD, cfg);
            }
        }
        lastNames = now;
    }

    private static boolean allowed(String name, String self, ModConfig cfg) {
        if (name.equals(self)) {
            return false;
        }
        return !cfg.notifyOnlyListed || ChatProcessor.lookup(name, cfg) != null;
    }

    private static void push(MinecraftClient mc, String name, String action, int color, ModConfig cfg) {
        ACTIVE.add(new Note(name, action, color, System.currentTimeMillis()));
        while (ACTIVE.size() > MAX_ACTIVE) {
            ACTIVE.remove(0);
        }
        if (cfg.notifySound) {
            ClickableWidget.playClickSound(mc.getSoundManager());
        }
    }

    public static List<Note> active() {
        long now = System.currentTimeMillis();
        ACTIVE.removeIf(n -> now - n.timeMs() > LIFETIME_MS);
        return new ArrayList<>(ACTIVE);
    }
}
