package dev.skinoverride;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Base class for our screens: draws and handles buttons in the shared dark/teal style. */
public abstract class ThemedScreen extends Screen {

    protected record Btn(int x, int y, int w, int h, Supplier<String> label, BooleanSupplier enabled,
                         boolean primary, Runnable action) {
    }

    protected final List<Btn> btns = new ArrayList<>();

    protected ThemedScreen(Text title) {
        super(title);
    }

    @Override
    protected final void init() {
        btns.clear();
        build();
    }

    /** Create widgets and buttons here (called on open and on every window resize). */
    protected abstract void build();

    protected void button(int x, int y, int w, int h, Supplier<String> label, BooleanSupplier enabled,
                          boolean primary, Runnable action) {
        btns.add(new Btn(x, y, w, h, label, enabled, primary, action));
    }

    protected void button(int x, int y, int w, int h, String label, boolean primary, Runnable action) {
        button(x, y, w, h, () -> label, () -> true, primary, action);
    }

    protected void drawButtons(DrawContext c, int mouseX, int mouseY) {
        for (Btn b : btns) {
            boolean enabled = b.enabled().getAsBoolean();
            boolean hovered = enabled && Ui.inside(mouseX, mouseY, b.x(), b.y(), b.w(), b.h());
            Ui.button(c, this.textRenderer, b.x(), b.y(), b.w(), b.h(), b.label().get(), hovered, enabled, b.primary());
        }
    }

    protected boolean clickButtons(Click click) {
        if (click.button() != 0) {
            return false;
        }
        for (Btn b : btns) {
            if (b.enabled().getAsBoolean() && Ui.inside(click.x(), click.y(), b.x(), b.y(), b.w(), b.h())) {
                clickSound();
                b.action().run();
                return true;
            }
        }
        return false;
    }

    protected void clickSound() {
        ClickableWidget.playClickSound(this.client.getSoundManager());
    }
}
