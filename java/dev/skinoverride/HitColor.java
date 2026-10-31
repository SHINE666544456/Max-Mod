package dev.skinoverride;

import dev.skinoverride.chat.ChatColors;
import dev.skinoverride.mixin.OverlayTextureAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

/**
 * Recolors the tint creatures flash when they get hurt. The game keeps that tint in a tiny 16x16 texture
 * (the top half is the hurt color), so changing the color means rewriting those pixels.
 */
public final class HitColor {

    private static final int VANILLA_RGB = 0xFF0000;
    private static final int VANILLA_ALPHA = 178;

    private static int lastSignature = Integer.MIN_VALUE;
    private static int ticks;

    private HitColor() {
    }

    public static void tick(MinecraftClient mc) {
        if (mc.gameRenderer == null) {
            return;
        }
        ModConfig cfg = ModConfig.get();
        int rgb = VANILLA_RGB;
        int alpha = VANILLA_ALPHA;
        if (cfg.hitColorOn && cfg.hitColor != ChatColors.DEFAULT) {
            int mode = cfg.hitColor;
            if (mode == ChatColors.RAINBOW) {
                rgb = ChatColors.hsv((System.currentTimeMillis() % 2500L) / 2500f, 0.75f, 1f);
            } else if (mode == ChatColors.CYCLE) {
                rgb = ChatColors.cycleColor();
            } else {
                rgb = mode & 0xFFFFFF;
            }
            alpha = Math.round(Math.max(10, Math.min(100, cfg.hitOpacity)) * 2.55f);
        } else if (cfg.hitColorOn) {
            alpha = Math.round(Math.max(10, Math.min(100, cfg.hitOpacity)) * 2.55f);
        }

        int signature = rgb * 31 + alpha;
        boolean refresh = ++ticks % 200 == 0; // the game may rebuild the texture, so re-apply now and then
        if (signature == lastSignature && !refresh) {
            return;
        }
        OverlayTexture overlay = mc.gameRenderer.getOverlayTexture();
        if (overlay == null) {
            return;
        }
        NativeImageBackedTexture texture = ((OverlayTextureAccessor) (Object) overlay).skinoverride$getTexture();
        NativeImage image = texture == null ? null : texture.getImage();
        if (image == null) {
            return;
        }
        int argb = (alpha << 24) | (rgb & 0xFFFFFF);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 16; x++) {
                image.setColorArgb(x, y, argb);
            }
        }
        texture.upload();
        lastSignature = signature;
    }
}
