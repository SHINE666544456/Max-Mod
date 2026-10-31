package dev.skinoverride;

import dev.skinoverride.hud.HudManager;
import dev.skinoverride.hud.HudWidget;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.Locale;

/**
 * Drag every HUD element where you want it. Scroll over an element to make it bigger or smaller,
 * right-click it to reset its size. Edges and the screen center snap.
 */
public class HudEditorScreen extends ThemedScreen {

    private static final int SNAP = 5;

    private final Screen parent;
    private HudWidget dragging;
    private int grabX;
    private int grabY;
    private boolean snapV;
    private boolean snapH;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("HUD layout"));
        this.parent = parent;
    }

    @Override
    protected void build() {
        int bw = 110;
        int y = this.height - 28;
        int startX = (this.width - (bw * 2 + 4)) / 2;
        button(startX, y, bw, 20, "Reset layout", false, () -> HudManager.resetLayout());
        button(startX + bw + 4, y, bw, 20, "Done", true, this::close);
    }

    /** Dim only: the normal blurred background would hide the layout. */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x88000000);
    }

    /** {x, y, scaledWidth, scaledHeight} of a widget's preview box. */
    private int[] rectOf(HudWidget w) {
        w.prepare(this.client, true);
        int ww = HudManager.scaledWidth(w);
        int wh = HudManager.scaledHeight(w);
        int[] p = HudManager.position(w, this.width, this.height, ww, wh);
        return new int[]{p[0], p[1], ww, wh};
    }

    private HudWidget widgetAt(double mx, double my) {
        // topmost (last drawn) widget under the mouse wins
        for (int i = HudManager.WIDGETS.size() - 1; i >= 0; i--) {
            HudWidget w = HudManager.WIDGETS.get(i);
            int[] r = rectOf(w);
            if (Ui.inside(mx, my, r[0], r[1], r[2], r[3])) {
                return w;
            }
        }
        return null;
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);

        if (dragging != null) {
            if (snapV) {
                c.fill(this.width / 2, 0, this.width / 2 + 1, this.height, Ui.withAlpha(Ui.ACCENT, 0x80));
            }
            if (snapH) {
                c.fill(0, this.height / 2, this.width, this.height / 2 + 1, Ui.withAlpha(Ui.ACCENT, 0x80));
            }
        }

        for (HudWidget w : HudManager.WIDGETS) {
            int[] r = rectOf(w);
            HudManager.renderWidget(c, this.client, w, r[0], r[1], true);

            boolean active = w == dragging;
            boolean hover = Ui.inside(mouseX, mouseY, r[0], r[1], r[2], r[3]);
            int border = active ? Ui.ACCENT : hover ? 0xFFFFFFFF : 0x80FFFFFF;
            c.fill(r[0] - 1, r[1] - 1, r[0] + r[2] + 1, r[1], border);
            c.fill(r[0] - 1, r[1] + r[3], r[0] + r[2] + 1, r[1] + r[3] + 1, border);
            c.fill(r[0] - 1, r[1], r[0], r[1] + r[3], border);
            c.fill(r[0] + r[2], r[1], r[0] + r[2] + 1, r[1] + r[3], border);

            String label = w.label() + "  x" + String.format(Locale.ROOT, "%.1f", HudManager.scale(w))
                    + (w.enabled() ? "" : "  (off)");
            int lw = this.textRenderer.getWidth(label) + 8;
            int ly = r[1] - 12 >= 0 ? r[1] - 12 : r[1] + r[3] + 2;
            Ui.roundRect(c, r[0], ly, lw, 11, Ui.withAlpha(Ui.BG, 0xE0));
            Ui.text(c, this.textRenderer, label, r[0] + 4, ly + 2, w.enabled() ? Ui.TEXT : Ui.MUTED);
        }

        String hint = "Drag to move  -  scroll over an element to resize  -  right-click to reset its size";
        Ui.text(c, this.textRenderer, hint, (this.width - this.textRenderer.getWidth(hint)) / 2, 8, Ui.TEXT);
        String hint2 = "Turn elements on or off in Settings";
        Ui.text(c, this.textRenderer, hint2, (this.width - this.textRenderer.getWidth(hint2)) / 2, 20, Ui.MUTED);

        drawButtons(c, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled) || clickButtons(click)) {
            return true;
        }
        HudWidget w = widgetAt(click.x(), click.y());
        if (w == null) {
            return false;
        }
        if (click.button() == 1) {
            resize(w, 1.0f, true);
            return true;
        }
        if (click.button() != 0) {
            return false;
        }
        int[] r = rectOf(w);
        dragging = w;
        grabX = (int) click.x() - r[0];
        grabY = (int) click.y() - r[1];
        return true;
    }

    /** Changes a widget's size while keeping its top-left corner where it is. */
    private void resize(HudWidget w, float newScale, boolean absolute) {
        int[] before = rectOf(w);
        HudManager.setScale(w, absolute ? newScale : HudManager.scale(w) + newScale);
        int ww = HudManager.scaledWidth(w);
        int wh = HudManager.scaledHeight(w);
        HudManager.setPosition(w, before[0], before[1], this.width, this.height, ww, wh);
        ModConfig.get().save();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        HudWidget w = widgetAt(mouseX, mouseY);
        if (w == null || verticalAmount == 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        resize(w, verticalAmount > 0 ? 0.1f : -0.1f, false);
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging == null) {
            return super.mouseDragged(click, offsetX, offsetY);
        }
        dragging.prepare(this.client, true);
        int ww = HudManager.scaledWidth(dragging);
        int wh = HudManager.scaledHeight(dragging);
        int x = (int) click.x() - grabX;
        int y = (int) click.y() - grabY;

        int minX = HudManager.PAD;
        int maxX = this.width - ww - HudManager.PAD;
        int minY = HudManager.PAD;
        int maxY = this.height - wh - HudManager.PAD;
        snapV = false;
        snapH = false;
        if (Math.abs(x - minX) < SNAP) {
            x = minX;
        } else if (Math.abs(x - maxX) < SNAP) {
            x = maxX;
        } else if (Math.abs(x + ww / 2 - this.width / 2) < SNAP) {
            x = this.width / 2 - ww / 2;
            snapV = true;
        }
        if (Math.abs(y - minY) < SNAP) {
            y = minY;
        } else if (Math.abs(y - maxY) < SNAP) {
            y = maxY;
        } else if (Math.abs(y + wh / 2 - this.height / 2) < SNAP) {
            y = this.height / 2 - wh / 2;
            snapH = true;
        }
        x = Math.max(minX, Math.min(Math.max(minX, maxX), x));
        y = Math.max(minY, Math.min(Math.max(minY, maxY), y));
        HudManager.setPosition(dragging, x, y, this.width, this.height, ww, wh);
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging != null) {
            dragging = null;
            snapV = false;
            snapH = false;
            ModConfig.get().save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public void close() {
        ModConfig.get().save();
        this.client.setScreen(this.parent);
    }
}
