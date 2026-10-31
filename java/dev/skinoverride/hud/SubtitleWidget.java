package dev.skinoverride.hud;

import dev.skinoverride.ModConfig;
import dev.skinoverride.Subtitles;
import dev.skinoverride.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Sound captions ("Zombie groans") rebuilt as a movable, resizable HUD element.
 * Arrows show on which side of you the sound is. Follows Minecraft's "Show Subtitles" option.
 */
public class SubtitleWidget implements HudWidget {

    private record Row(String text, int dir, float fade) {
    }

    private final List<Row> rows = new ArrayList<>();
    private int w;
    private int h;

    @Override
    public String id() {
        return "subtitles";
    }

    @Override
    public String label() {
        return "Subtitles";
    }

    @Override
    public float defaultX() {
        return 1f;
    }

    @Override
    public float defaultY() {
        return 0.82f;
    }

    @Override
    public boolean enabled() {
        return ModConfig.get().subtitlesHud;
    }

    @Override
    public void prepare(MinecraftClient mc, boolean preview) {
        rows.clear();
        if (preview || mc.player == null) {
            rows.add(new Row("Zombie groans", -1, 1f));
            rows.add(new Row("Footsteps", 0, 0.8f));
            rows.add(new Row("Chest opens", 1, 0.55f));
        } else {
            Subtitles.ensureRegistered(mc);
            if (!mc.options.getShowSubtitles().getValue()) {
                w = 0;
                h = 0;
                return;
            }
            Vec3d eye = mc.player.getEyePos();
            double yaw = Math.toRadians(mc.player.getYaw());
            double rightX = -Math.cos(yaw);
            double rightZ = -Math.sin(yaw);
            for (Subtitles.Line line : Subtitles.snapshot()) {
                double dx = line.pos().x - eye.x;
                double dz = line.pos().z - eye.z;
                double distSq = dx * dx + (line.pos().y - eye.y) * (line.pos().y - eye.y) + dz * dz;
                if (distSq > line.range() * line.range()) {
                    continue;
                }
                double horizontal = Math.sqrt(dx * dx + dz * dz);
                int dir = 0;
                if (horizontal > 1.0) {
                    double side = (dx * rightX + dz * rightZ) / horizontal;
                    dir = side > 0.5 ? 1 : side < -0.5 ? -1 : 0;
                }
                float fade = 1f - Math.max(0f, (line.ageMs() - (Subtitles.LIFETIME_MS - 1000)) / 1000f);
                rows.add(new Row(line.text().getString(), dir, Math.max(0.15f, fade)));
            }
        }
        if (rows.isEmpty()) {
            w = 0;
            h = 0;
            return;
        }
        int max = 0;
        for (Row r : rows) {
            max = Math.max(max, mc.textRenderer.getWidth(r.text()));
        }
        w = max + 2 * 14 + 6;
        h = rows.size() * 12 - 2;
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
        int cy = y;
        for (Row r : rows) {
            int alpha = Math.round(255 * r.fade());
            Ui.roundRect(c, x, cy, w, 10, Ui.withAlpha(Ui.BG, Math.min(0xE0, alpha)));
            int textColor = Ui.withAlpha(0xFFFFFF, Math.max(alpha, 24));
            int tw = tr.getWidth(r.text());
            c.drawTextWithShadow(tr, r.text(), x + (w - tw) / 2, cy + 1, textColor);
            int arrowColor = Ui.withAlpha(Ui.ACCENT, Math.max(alpha, 24));
            if (r.dir() < 0) {
                c.drawTextWithShadow(tr, "<", x + 4, cy + 1, arrowColor);
            } else if (r.dir() > 0) {
                c.drawTextWithShadow(tr, ">", x + w - 4 - tr.getWidth(">"), cy + 1, arrowColor);
            }
            cy += 12;
        }
    }
}
