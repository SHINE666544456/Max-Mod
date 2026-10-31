package dev.skinoverride;

import net.minecraft.client.texture.NativeImage;

/**
 * Prepares a skin PNG the same way vanilla does when it downloads one:
 * converts old 64x32 skins to 64x64 and makes the base layers opaque.
 * Works with HD skins too (128x128, 256x256, ...).
 */
public final class SkinImages {

    private SkinImages() {
    }

    /** Returns the scale factor (1 for 64x64, 2 for 128x128...) or throws if the size is invalid. */
    public static int scaleOf(NativeImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        if (w < 64 || w % 64 != 0) {
            throw new IllegalArgumentException("width " + w + " px (expected 64, 128, 256...)");
        }
        if (h != w && h * 2 != w) {
            throw new IllegalArgumentException("size " + w + "x" + h + " (expected " + w + "x" + w + " or " + w + "x" + (w / 2) + ")");
        }
        return w / 64;
    }

    public static boolean isLegacy(NativeImage image) {
        return image.getHeight() * 2 == image.getWidth();
    }

    /** Guesses the slim (Alex, 3px arms) model: those skins leave the 4th arm pixel column transparent. */
    public static boolean detectSlim(NativeImage image, int s) {
        if (isLegacy(image)) {
            return false;
        }
        return isBlank(image, 50 * s, 16 * s, 2 * s, 4 * s)
                && isBlank(image, 54 * s, 20 * s, 2 * s, 12 * s)
                && isBlank(image, 42 * s, 48 * s, 2 * s, 4 * s)
                && isBlank(image, 46 * s, 52 * s, 2 * s, 12 * s);
    }

    /** Returns a NEW square image ready for the GPU. The source is not modified. */
    public static NativeImage process(NativeImage source) {
        int s = scaleOf(source);
        int size = 64 * s;
        boolean legacy = isLegacy(source);

        NativeImage out = new NativeImage(size, size, true);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < source.getHeight(); y++) {
                out.setColorArgb(x, y, source.getColorArgb(x, y));
            }
        }

        if (legacy) {
            out.fillRect(0, 32 * s, 64 * s, 32 * s, 0);
            // right leg -> left leg (mirrored)
            copy(out, s, 4, 16, 16, 32, 4, 4);
            copy(out, s, 8, 16, 16, 32, 4, 4);
            copy(out, s, 0, 20, 24, 32, 4, 12);
            copy(out, s, 4, 20, 16, 32, 4, 12);
            copy(out, s, 8, 20, 8, 32, 4, 12);
            copy(out, s, 12, 20, 16, 32, 4, 12);
            // right arm -> left arm (mirrored)
            copy(out, s, 44, 16, -8, 32, 4, 4);
            copy(out, s, 48, 16, -8, 32, 4, 4);
            copy(out, s, 40, 20, 0, 32, 4, 12);
            copy(out, s, 44, 20, -8, 32, 4, 12);
            copy(out, s, 48, 20, -16, 32, 4, 12);
            copy(out, s, 52, 20, -8, 32, 4, 12);
        }

        forceOpaque(out, 0, 0, 32 * s, 16 * s);
        if (legacy) {
            clearIfOpaque(out, 32 * s, 0, 64 * s, 32 * s);
        }
        forceOpaque(out, 0, 16 * s, 64 * s, 32 * s);
        forceOpaque(out, 16 * s, 48 * s, 48 * s, 64 * s);
        return out;
    }

    private static void copy(NativeImage image, int s, int x, int y, int dx, int dy, int w, int h) {
        image.copyRect(x * s, y * s, dx * s, dy * s, w * s, h * s, true, false);
    }

    private static boolean isBlank(NativeImage image, int x, int y, int w, int h) {
        for (int i = x; i < x + w; i++) {
            for (int j = y; j < y + h; j++) {
                if ((image.getColorArgb(i, j) >>> 24) != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void forceOpaque(NativeImage image, int x1, int y1, int x2, int y2) {
        for (int x = x1; x < x2; x++) {
            for (int y = y1; y < y2; y++) {
                image.setColorArgb(x, y, image.getColorArgb(x, y) | 0xFF000000);
            }
        }
    }

    private static void clearIfOpaque(NativeImage image, int x1, int y1, int x2, int y2) {
        for (int x = x1; x < x2; x++) {
            for (int y = y1; y < y2; y++) {
                if ((image.getColorArgb(x, y) >>> 24) < 128) {
                    return;
                }
            }
        }
        for (int x = x1; x < x2; x++) {
            for (int y = y1; y < y2; y++) {
                image.setColorArgb(x, y, image.getColorArgb(x, y) & 0x00FFFFFF);
            }
        }
    }
}
