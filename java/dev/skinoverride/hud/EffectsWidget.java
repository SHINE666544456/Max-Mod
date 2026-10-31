package dev.skinoverride.hud;

import dev.skinoverride.ModConfig;
import dev.skinoverride.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Status effects with name, level and a countdown. The bar on the left drains and shifts from
 * green through yellow and orange to red as the effect runs out. Flashes red when about to expire.
 */
public class EffectsWidget implements HudWidget {

    private static final int ROW_H = 24;
    private static final int ROW_GAP = 2;
    private static final int MAX_ROWS = 10;
    private static final int BAR_TOP = 3;
    private static final int BAR_H = ROW_H - 6;

    private record Row(Identifier sprite, String name, int ticks, boolean infinite, float frac) {
    }

    /** Longest duration seen for each active effect, so the bar knows what "full" means. */
    private static final Map<String, Integer> MAX_SEEN = new HashMap<>();

    private final List<Row> rows = new ArrayList<>();
    private int w;
    private int h;

    @Override
    public String id() {
        return "effects";
    }

    @Override
    public String label() {
        return "Effect timers";
    }

    @Override
    public float defaultX() {
        return 1f;
    }

    @Override
    public float defaultY() {
        return 0f;
    }

    @Override
    public boolean enabled() {
        return ModConfig.get().effectsHud;
    }

    @Override
    public void prepare(MinecraftClient mc, boolean preview) {
        rows.clear();
        if (preview || mc.player == null) {
            rows.add(new Row(sprite("speed"), "Speed II", 20 * 95, false, 0.80f));
            rows.add(new Row(sprite("regeneration"), "Regeneration", 20 * 14, false, 0.45f));
            rows.add(new Row(sprite("poison"), "Poison", 20 * 6, false, 0.15f));
        } else {
            Set<String> seen = new HashSet<>();
            for (StatusEffectInstance inst : mc.player.getStatusEffects()) {
                if (!inst.shouldShowIcon() || rows.size() >= MAX_ROWS) {
                    continue;
                }
                Identifier key = inst.getEffectType().getKey()
                        .map(k -> k.getValue())
                        .orElse(Identifier.ofVanilla("speed"));
                String name = inst.getEffectType().value().getName().getString();
                if (inst.getAmplifier() > 0) {
                    name += " " + roman(inst.getAmplifier() + 1);
                }
                String mapKey = key + ":" + inst.getAmplifier();
                seen.add(mapKey);
                int ticks = inst.getDuration();
                int max = MAX_SEEN.merge(mapKey, ticks, Math::max);
                float frac = inst.isInfinite() ? 1f : Math.max(0f, Math.min(1f, ticks / (float) Math.max(1, max)));
                rows.add(new Row(
                        Identifier.of(key.getNamespace(), "mob_effect/" + key.getPath()),
                        name, ticks, inst.isInfinite(), frac));
            }
            MAX_SEEN.keySet().retainAll(seen);
        }
        if (rows.isEmpty()) {
            w = 0;
            h = 0;
            return;
        }
        int max = 0;
        for (Row r : rows) {
            max = Math.max(max, Math.max(
                    mc.textRenderer.getWidth(r.name()),
                    mc.textRenderer.getWidth(timeText(r))));
        }
        w = 7 + 18 + 5 + max + 6;
        h = rows.size() * (ROW_H + ROW_GAP) - ROW_GAP;
    }

    private static Identifier sprite(String name) {
        return Identifier.ofVanilla("mob_effect/" + name);
    }

    private static String roman(int n) {
        String[] numerals = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n >= 1 && n < numerals.length ? numerals[n] : String.valueOf(n);
    }

    private static String timeText(Row r) {
        return r.infinite() ? "inf" : Ui.formatSeconds((r.ticks() + 19) / 20);
    }

    /** Green (full) -> yellow -> orange -> red (empty). */
    public static int timeColor(float f) {
        if (f >= 0.66f) {
            return Ui.mix(0xFFFF55, 0x55FF55, (f - 0.66f) / 0.34f);
        }
        if (f >= 0.33f) {
            return Ui.mix(0xFFAA00, 0xFFFF55, (f - 0.33f) / 0.33f);
        }
        return Ui.mix(0xFF3333, 0xFFAA00, f / 0.33f);
    }

    @Override
    public int width() {
        return w;
    }

    @Override
    public int height() {
        return h;
    }

    @Override
    public void render(DrawContext c, MinecraftClient mc, int x, int y, boolean preview) {
        ModConfig cfg = ModConfig.get();
        int cy = y;
        for (Row r : rows) {
            boolean low = !r.infinite() && r.ticks() <= cfg.effectsLowSeconds * 20;
            boolean flash = low && cfg.effectsFlash;
            boolean on = flash && Ui.blink();

            Ui.roundRect(c, x, cy, w, ROW_H, on ? 0xC0501818 : Ui.BG);
            int border = low ? (on || !cfg.effectsFlash ? Ui.BAD : 0xFF7A2B2B) : Ui.BORDER;
            Ui.roundBorder(c, x, cy, w, ROW_H, border);

            // draining time bar
            c.fill(x + 2, cy + BAR_TOP, x + 5, cy + BAR_TOP + BAR_H, 0xFF171B27);
            int fill = Math.round(BAR_H * r.frac());
            if (fill > 0) {
                c.fill(x + 2, cy + BAR_TOP + BAR_H - fill, x + 5, cy + BAR_TOP + BAR_H, 0xFF000000 | timeColor(r.frac()));
            }

            c.drawGuiTexture(RenderPipelines.GUI_TEXTURED, r.sprite(), x + 7, cy + 3, 18, 18);

            c.drawTextWithShadow(mc.textRenderer, r.name(), x + 30, cy + 3, Ui.TEXT);
            int timeColor = low ? (on || !cfg.effectsFlash ? Ui.BAD : 0xFFB04040) : Ui.MUTED;
            c.drawTextWithShadow(mc.textRenderer, timeText(r), x + 30, cy + 13, timeColor);
            cy += ROW_H + ROW_GAP;
        }
    }
}
