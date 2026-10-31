package dev.skinoverride.hud;

import dev.skinoverride.AfkTracker;
import dev.skinoverride.ModConfig;
import dev.skinoverride.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

/** Small list of nearby players currently marked AFK. Hidden when nobody is AFK. */
public class AfkWidget implements HudWidget {

    private record Entry(String name, String time) {
    }

    private final List<Entry> entries = new ArrayList<>();
    private int w;
    private int h;

    @Override
    public String id() {
        return "afk";
    }

    @Override
    public String label() {
        return "AFK list";
    }

    @Override
    public float defaultX() {
        return 0f;
    }

    @Override
    public float defaultY() {
        return 0.3f;
    }

    @Override
    public boolean enabled() {
        ModConfig cfg = ModConfig.get();
        return cfg.afkTracking && cfg.afkHudList;
    }

    @Override
    public void prepare(MinecraftClient mc, boolean preview) {
        entries.clear();
        if (preview || mc.world == null) {
            entries.add(new Entry("Alex", "4m"));
            entries.add(new Entry("Sam", "1h 05m"));
        } else {
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p != mc.player && AfkTracker.isAfk(p.getUuid())) {
                    entries.add(new Entry(p.getName().getString(), Ui.formatShort(AfkTracker.idleSeconds(p.getUuid()))));
                }
            }
        }
        if (entries.isEmpty()) {
            w = 0;
            h = 0;
            return;
        }
        int max = mc.textRenderer.getWidth("AFK players");
        for (Entry e : entries) {
            max = Math.max(max, mc.textRenderer.getWidth(e.name()) + 12 + mc.textRenderer.getWidth(e.time()));
        }
        w = max + 10;
        h = 4 + 11 + entries.size() * 10 + 3;
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
        c.fill(x + 1, y + 1, x + w - 1, y + 2, Ui.WARN);
        c.drawTextWithShadow(tr, "AFK players", x + 5, y + 5, Ui.WARN);
        int ly = y + 4 + 11;
        for (Entry e : entries) {
            c.drawTextWithShadow(tr, e.name(), x + 5, ly, Ui.TEXT);
            c.drawTextWithShadow(tr, e.time(), x + w - 5 - tr.getWidth(e.time()), ly, Ui.MUTED);
            ly += 10;
        }
    }
}
