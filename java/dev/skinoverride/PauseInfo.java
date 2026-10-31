package dev.skinoverride;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** The day / time / playtime panel drawn on the pause menu. */
public final class PauseInfo {

    private static final DateTimeFormatter REAL = DateTimeFormatter.ofPattern("HH:mm:ss");

    private PauseInfo() {
    }

    public static void render(DrawContext c, MinecraftClient mc, float delta) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.pauseInfo || mc.world == null || !DayClock.valid()) {
            return;
        }
        TextRenderer tr = mc.textRenderer;
        float tp = 0f;
        int dayTick = DayClock.dayTick(tp);

        List<String[]> lines = new ArrayList<>(); // {label, value}
        lines.add(new String[]{"Time", DayClock.clock(tp, cfg.clock24h, true) + "  (" + DayClock.phase(dayTick) + ")"});
        if (DayClock.running()) {
            lines.add(new String[]{dayTick < 12000 ? "Sunset in" : "Sunrise in",
                    Ui.formatSeconds(DayClock.secondsUntil(dayTick, dayTick < 12000 ? 12000 : 0))});
            lines.add(new String[]{"Next day in", Ui.formatSeconds(DayClock.secondsUntil(dayTick, 0))});
        } else {
            lines.add(new String[]{"Clock", "stopped"});
        }
        lines.add(new String[]{"", ""});
        lines.add(new String[]{"Played here", PlayTime.format(PlayTime.worldTicks())});
        lines.add(new String[]{"This session", PlayTime.format(PlayTime.sessionTicks())});
        lines.add(new String[]{"All worlds", PlayTime.format(PlayTime.allTicks())});
        lines.add(new String[]{"Real time", LocalTime.now().format(REAL)});

        int labelW = 0;
        int valueW = 0;
        for (String[] l : lines) {
            labelW = Math.max(labelW, tr.getWidth(l[0]));
            valueW = Math.max(valueW, tr.getWidth(l[1]));
        }
        String dayText = "Day " + DayClock.day(tp);
        int w = Math.max(labelW + 12 + valueW, tr.getWidth(dayText) * 2) + 16;
        int h = 30 + lines.size() * 11 + 6;
        int x = 8;
        int y = 8;

        Ui.panel(c, x, y, w, h);
        c.fill(x + 1, y + 1, x + w - 1, y + 2, Ui.ACCENT);

        var m = c.getMatrices();
        m.pushMatrix();
        m.translate((float) (x + 8), (float) (y + 8));
        m.scale(2f, 2f);
        c.drawTextWithShadow(tr, dayText, 0, 0, Ui.ACCENT);
        m.popMatrix();

        int ty = y + 30;
        for (String[] l : lines) {
            if (!l[0].isEmpty()) {
                c.drawTextWithShadow(tr, l[0], x + 8, ty, Ui.MUTED);
                c.drawTextWithShadow(tr, l[1], x + w - 8 - tr.getWidth(l[1]), ty, Ui.TEXT);
            }
            ty += 11;
        }
    }
}
