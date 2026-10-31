package dev.skinoverride.hud;

import dev.skinoverride.ModConfig;
import dev.skinoverride.Notifications;
import dev.skinoverride.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Small pop-ups when players join or leave. */
public class NotificationWidget implements HudWidget {

    private record Row(String name, String action, int color, float fade) {
    }

    private static final int ROW_H = 24;

    private final List<Row> rows = new ArrayList<>();
    private int w;
    private int h;

    @Override
    public String id() {
        return "notifications";
    }

    @Override
    public String label() {
        return "Join / leave pop-ups";
    }

    @Override
    public float defaultX() {
        return 1f;
    }

    @Override
    public float defaultY() {
        return 0.2f;
    }

    @Override
    public boolean enabled() {
        return ModConfig.get().notifyPopups;
    }

    @Override
    public void prepare(MinecraftClient mc, boolean preview) {
        rows.clear();
        if (preview || mc.player == null) {
            rows.add(new Row("Alex", "joined", Ui.GOOD, 1f));
            rows.add(new Row("Sam", "left", Ui.BAD, 1f));
        } else {
            long now = System.currentTimeMillis();
            for (Notifications.Note n : Notifications.active()) {
                long age = now - n.timeMs();
                float fade = age < 250 ? age / 250f
                        : age > Notifications.LIFETIME_MS - 600 ? (Notifications.LIFETIME_MS - age) / 600f : 1f;
                rows.add(new Row(n.name(), n.action(), n.color(), Math.max(0.1f, Math.min(1f, fade))));
            }
        }
        if (rows.isEmpty()) {
            w = 0;
            h = 0;
            return;
        }
        int max = mc.textRenderer.getWidth("joined");
        for (Row r : rows) {
            max = Math.max(max, mc.textRenderer.getWidth(r.name()));
        }
        w = 10 + 12 + max + 10;
        h = rows.size() * (ROW_H + 2) - 2;
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
        int cy = y;
        for (Row r : rows) {
            int alpha = Math.round(255 * r.fade());
            Ui.roundRect(c, x, cy, w, ROW_H, Ui.withAlpha(Ui.BG, Math.min(0xE6, alpha)));
            Ui.roundBorder(c, x, cy, w, ROW_H, Ui.withAlpha(r.color(), alpha));
            c.fill(x + 3, cy + 4, x + 5, cy + ROW_H - 4, Ui.withAlpha(r.color(), alpha));
            c.drawTextWithShadow(mc.textRenderer, r.name(), x + 10, cy + 4, Ui.withAlpha(0xE8ECF4, Math.max(alpha, 24)));
            c.drawTextWithShadow(mc.textRenderer, r.action(), x + 10, cy + 13, Ui.withAlpha(r.color(), Math.max(alpha, 24)));
            cy += ROW_H + 2;
        }
    }
}
