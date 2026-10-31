package dev.skinoverride.hud;

import dev.skinoverride.DayClock;
import dev.skinoverride.ModConfig;
import dev.skinoverride.PlayTime;
import dev.skinoverride.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Permanent day counter and ticking in-game clock. */
public class ClockWidget implements HudWidget {

    private final List<String> lines = new ArrayList<>();
    private String dayLine = "";
    private int w;
    private int h;

    @Override
    public String id() {
        return "clock";
    }

    @Override
    public String label() {
        return "Day & time";
    }

    @Override
    public float defaultX() {
        return 0.5f;
    }

    @Override
    public float defaultY() {
        return 0f;
    }

    @Override
    public boolean enabled() {
        return ModConfig.get().clockHud;
    }

    @Override
    public void prepare(MinecraftClient mc, boolean preview) {
        ModConfig cfg = ModConfig.get();
        lines.clear();
        boolean demo = preview || mc.world == null || !DayClock.valid();
        float tp = demo ? 0f : mc.getRenderTickCounter().getTickProgress(false);

        dayLine = "Day " + (demo ? 12 : DayClock.day(tp));
        String time = demo
                ? (cfg.clock24h ? (cfg.clockSeconds ? "14:23:45" : "14:23") : (cfg.clockSeconds ? "02:23:45 PM" : "02:23 PM"))
                : DayClock.clock(tp, cfg.clock24h, cfg.clockSeconds);
        lines.add(time + "  " + (demo ? "Afternoon" : DayClock.phase(DayClock.dayTick(tp))));
        if (cfg.clockPlaytime) {
            lines.add("Played " + (demo ? "12h 03m" : PlayTime.format(PlayTime.worldTicks())));
        }

        int max = mc.textRenderer.getWidth(dayLine);
        for (String l : lines) {
            max = Math.max(max, mc.textRenderer.getWidth(l));
        }
        w = max + 16;
        h = 5 + 11 + lines.size() * 11 + 3;
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
        var tr = mc.textRenderer;
        Ui.panel(c, x, y, w, h);
        c.fill(x + 1, y + 1, x + w - 1, y + 2, Ui.ACCENT);
        c.drawTextWithShadow(tr, dayLine, x + 8, y + 5, Ui.ACCENT);
        int ty = y + 16;
        for (String l : lines) {
            c.drawTextWithShadow(tr, l, x + 8, ty, Ui.TEXT);
            ty += 11;
        }
    }
}
