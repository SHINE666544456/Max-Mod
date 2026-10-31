package dev.skinoverride;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.NarratorMode;

/**
 * Text-to-speech for chat uses Minecraft's own built-in narrator (set to "Chat"), so the voice and
 * speed come from your operating system. Turning it off restores your previous narrator setting.
 */
public final class Tts {

    private Tts() {
    }

    public static void set(boolean on) {
        ModConfig cfg = ModConfig.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        cfg.chatTts = on;
        cfg.save();
        if (mc.options == null) {
            return;
        }
        NarratorMode current = mc.options.getNarrator().getValue();
        if (on) {
            if (current != NarratorMode.CHAT && current != NarratorMode.ALL) {
                cfg.ttsPrevMode = current.name();
                cfg.save();
                mc.options.getNarrator().setValue(NarratorMode.CHAT);
            }
        } else {
            try {
                mc.options.getNarrator().setValue(NarratorMode.valueOf(cfg.ttsPrevMode));
            } catch (IllegalArgumentException e) {
                mc.options.getNarrator().setValue(NarratorMode.OFF);
            }
        }
    }
}
