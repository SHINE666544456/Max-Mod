package dev.skinoverride.hud;

import dev.skinoverride.Freecam;
import dev.skinoverride.ModConfig;
import dev.skinoverride.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Small "FREECAM" indicator with the current speed, shown only while freecam is active. */
public class FreecamWidget implements HudWidget {

    private String line1 = "FREECAM";
    private String line2 = "";
    private int w;
    private int h;

    @Override
    public String id() {
        return "freecam";
    }

    @Override
    public String label() {
        return "Freecam indicator";
    }

    @Override
    public float defaultX() {
        return 0.5f;
    }

    @Override
    public float defaultY() {
        return 0.08f;
    }

    @Override
    public boolean enabled() {
        return ModConfig.get().freecamIndicator;
    }

    @Override
    public boolean visible(MinecraftClient mc) {
        return Freecam.active();
    }

    @Override
    public void prepare(MinecraftClient mc, boolean preview) {
        line1 = "FREECAM";
        line2 = ModConfig.get().freecamSpeed + " blocks/s  -  scroll to change";
        w = Math.max(mc.textRenderer.getWidth(line1), mc.textRenderer.getWidth(line2)) + 16;
        h = 5 + 11 + 11 + 3;
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
        Ui.panel(c, x, y, w, h);
        c.fill(x + 1, y + 1, x + w - 1, y + 2, Ui.ACCENT);
        c.drawTextWithShadow(mc.textRenderer, line1, x + (w - mc.textRenderer.getWidth(line1)) / 2, y + 5, Ui.ACCENT);
        c.drawTextWithShadow(mc.textRenderer, line2, x + (w - mc.textRenderer.getWidth(line2)) / 2, y + 16, Ui.MUTED);
    }
}
