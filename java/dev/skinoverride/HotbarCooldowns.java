package dev.skinoverride;

import dev.skinoverride.chat.ChatColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.joml.Matrix3x2fStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Makes item cooldowns easy to read on the hotbar: a colored wipe over the item, the seconds left,
 * and a quick flash when the item is ready again.
 */
public final class HotbarCooldowns {

    private static final class Track {
        float lastProgress;
        float startProgress;
        long startMs;
        float totalSeconds = -1f;
        long flashUntil;
    }

    private static final Map<Item, Track> TRACKS = new HashMap<>();

    private HotbarCooldowns() {
    }

    public static void render(DrawContext c, MinecraftClient mc) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.cooldownOverlay || mc.player == null) {
            return;
        }
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        int baseX = sw / 2 - 90 + 2;
        int y = sh - 19;

        ItemCooldownManager manager = mc.player.getItemCooldownManager();
        float tp = mc.getRenderTickCounter().getTickProgress(false);
        long now = System.currentTimeMillis();
        TextRenderer tr = mc.textRenderer;

        int rgb = cfg.cooldownColor == ChatColors.DEFAULT ? (Ui.ACCENT & 0xFFFFFF)
                : cfg.cooldownColor == ChatColors.RAINBOW ? ChatColors.hsv((now % 2000L) / 2000f, 0.7f, 1f)
                : cfg.cooldownColor == ChatColors.CYCLE ? ChatColors.cycleColor()
                : cfg.cooldownColor;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            int x = baseX + i * 20;
            float p = manager.getCooldownProgress(stack, tp);
            Track t = TRACKS.computeIfAbsent(stack.getItem(), k -> new Track());

            if (p > 0.001f) {
                if (t.lastProgress <= 0.001f || p > t.lastProgress + 0.03f) {
                    t.startProgress = p;
                    t.startMs = now;
                    t.totalSeconds = -1f;
                }
                float used = t.startProgress - p;
                if (used >= 0.04f) {
                    t.totalSeconds = ((now - t.startMs) / 1000f) / used;
                }
                t.lastProgress = p;

                int height = Math.round(16 * p);
                c.fill(x, y, x + 16, y + height, Ui.withAlpha(rgb, 0x78));
                c.fill(x, y + height - 1, x + 16, y + height, Ui.withAlpha(rgb, 0xE0));

                if (cfg.cooldownNumbers && t.totalSeconds > 0f) {
                    float remaining = p * t.totalSeconds;
                    String text = remaining >= 10f
                            ? String.valueOf(Math.round(remaining))
                            : String.format(Locale.ROOT, "%.1f", remaining);
                    Matrix3x2fStack m = c.getMatrices();
                    m.pushMatrix();
                    m.translate((float) (x + 8), (float) (y + 4));
                    m.scale(0.85f, 0.85f);
                    c.drawTextWithShadow(tr, text, -tr.getWidth(text) / 2, 0, 0xFFFFFFFF);
                    m.popMatrix();
                }
            } else {
                if (t.lastProgress > 0.001f) {
                    t.flashUntil = now + 450L;
                }
                t.lastProgress = 0f;
                if (cfg.cooldownReadyFlash && now < t.flashUntil) {
                    float left = (t.flashUntil - now) / 450f;
                    int color = Ui.withAlpha(0x55FF55, Math.max(30, (int) (255 * left)));
                    c.fill(x - 1, y - 1, x + 17, y, color);
                    c.fill(x - 1, y + 16, x + 17, y + 17, color);
                    c.fill(x - 1, y, x, y + 16, color);
                    c.fill(x + 16, y, x + 17, y + 16, color);
                }
            }
        }
    }
}
