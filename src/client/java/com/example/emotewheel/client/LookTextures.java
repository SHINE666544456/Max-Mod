package com.example.emotewheel.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;

/**
 * 64x64 atlas for ears, tail and the head pet. Built from the hex colours in settings
 * (outer / inner / tail), with darker and lighter neighbours mixed in so fur isn't flat.
 */
public final class LookTextures {
    private LookTextures() {}

    public static final Identifier ID = Identifier.fromNamespaceAndPath("emote_wheel", "dynamic/look");
    public static final Identifier DEFAULT_ID = Identifier.fromNamespaceAndPath("emote_wheel", "dynamic/look_default");

    public static final int OUTER = 0x2C1A12;
    public static final int INNER = 0xE8A0B0;
    public static final int TAIL = 0x2C1A12;

    private static DynamicTexture custom, fallback;
    private static int builtOuter = -1, builtInner = -1, builtTail = -1;

    public static Identifier self() {
        rebuild();
        return ID;
    }

    public static Identifier others() { return DEFAULT_ID; }

    public static void rebuild() {
        // DynamicTexture needs the render device, so create textures lazily on first render/use.
        if (fallback == null) fallback = upload(DEFAULT_ID, OUTER, INNER, TAIL);

        EmoteConfig c = EmoteConfig.get();
        int o = parse(c.earOuter, OUTER);
        int i = parse(c.earInner, INNER);
        int t = parse(c.tailColor, TAIL);
        if (custom != null && o == builtOuter && i == builtInner && t == builtTail) return;
        builtOuter = o; builtInner = i; builtTail = t;
        custom = upload(ID, o, i, t);
    }

    public static int parse(String hex, int fallbackRgb) {
        if (hex == null) return fallbackRgb;
        String s = hex.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() == 3) {
            s = "" + s.charAt(0) + s.charAt(0) + s.charAt(1) + s.charAt(1) + s.charAt(2) + s.charAt(2);
        }
        if (s.length() != 6) return fallbackRgb;
        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallbackRgb;
        }
    }

    public static String format(int rgb) {
        return String.format("%06X", rgb & 0xFFFFFF);
    }

    private static DynamicTexture upload(Identifier id, int outer, int inner, int tail) {
        NativeImage img = paint(outer, inner, tail);
        DynamicTexture tex = new DynamicTexture(() -> null, img);
        Minecraft.getInstance().getTextureManager().register(id, tex);
        return tex;
    }

    /**
     * Atlas (64x64):
     *  0,0  fur (outer)     16,0 inner ear     32,0 belly/light     48,0 dark fur
     *  0,16 tail mid        16,16 eyes open    32,16 eyes shut     48,16 pads/nose
     */
    private static NativeImage paint(int outer, int inner, int tail) {
        NativeImage img = new NativeImage(64, 64, false);
        int dark = shade(outer, 0.55f);
        int light = shade(outer, 1.45f);
        int tailDark = shade(tail, 0.62f);
        int tailLight = shade(tail, 1.38f);
        int tailMid = mix(tail, outer, 0.35f);
        fill(img, 0, 0, 16, 16, outer, dark, light);          // fur
        fill(img, 16, 0, 16, 16, inner, shade(inner, 0.75f), shade(inner, 1.2f));
        fill(img, 32, 0, 16, 16, light, outer, shade(light, 1.15f)); // belly
        fill(img, 48, 0, 16, 16, dark, shade(dark, 0.8f), outer);
        fill(img, 0, 16, 16, 16, tailMid, tailDark, tailLight);     // tail surround
        // open eyes: pale with dark pupil
        fill(img, 16, 16, 16, 16, 0xF2E8D8, 0xF2E8D8, 0xF2E8D8);
        disc(img, 20, 20, 3, 0x1A1210);
        disc(img, 28, 20, 3, 0x1A1210);
        // shut eyes: thin dark line
        fill(img, 32, 16, 16, 16, outer, dark, light);
        for (int x = 34; x < 46; x++) {
            img.setPixel(x, 23, abgr(0x1A1210));
            img.setPixel(x, 24, abgr(0x1A1210));
        }
        fill(img, 48, 16, 16, 16, shade(inner, 0.55f), 0x3A1020, shade(inner, 0.8f)); // pads
        return img;
    }

    private static void fill(NativeImage img, int x0, int y0, int w, int h, int base, int dark, int light) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int n = (x * 13 + y * 31) & 7;
                int c = n < 2 ? dark : n > 5 ? light : base;
                // slight vertical gradient so tails read as round
                float v = y / (float) Math.max(1, h - 1);
                c = mix(c, dark, v * 0.25f);
                img.setPixel(x0 + x, y0 + y, abgr(c | 0xFF000000));
            }
        }
    }

    private static void disc(NativeImage img, int cx, int cy, int r, int rgb) {
        int rr = r * r;
        for (int y = -r; y <= r; y++)
            for (int x = -r; x <= r; x++)
                if (x * x + y * y <= rr) img.setPixel(cx + x, cy + y, abgr(rgb));
    }

    static int shade(int rgb, float f) {
        int r = clamp((int) (((rgb >> 16) & 255) * f));
        int g = clamp((int) (((rgb >> 8) & 255) * f));
        int b = clamp((int) ((rgb & 255) * f));
        return (r << 16) | (g << 8) | b;
    }

    static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int r = clamp((int) (ar + (br - ar) * t));
        int g = clamp((int) (ag + (bg - ag) * t));
        int bl = clamp((int) (ab + (bb - ab) * t));
        return (r << 16) | (g << 8) | bl;
    }

    private static int clamp(int v) { return v < 0 ? 0 : Math.min(255, v); }

    /** NativeImage pixels are ABGR. */
    private static int abgr(int argbOrRgb) {
        int a = (argbOrRgb >>> 24) == 0 ? 0xFF : (argbOrRgb >>> 24);
        int r = (argbOrRgb >> 16) & 255, g = (argbOrRgb >> 8) & 255, b = argbOrRgb & 255;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }
}
