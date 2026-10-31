package com.example.emotewheel.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * A button drawn in the Iris theme (rounded dark box, accent border on hover). Use {@link #builder}
 * exactly like {@code Button.builder}.
 */
public class ThemedButton extends Button.Plain {

    private boolean accented;
    private int inset;

    private ThemedButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    /** Highlights the button (selected item). */
    public ThemedButton setAccented(boolean accented) {
        this.accented = accented;
        return this;
    }

    @Override
    protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float delta) {
        IrisTheme.refresh();
        int x = getX();
        int y = getY();
        int w = this.width;
        int h = this.height;
        boolean hovered = this.isHovered() && this.active;

        // cover the vanilla button graphic completely
        g.fill(x, y, x + w, y + h, 0xFF000000 | (IrisTheme.bg() & 0xFFFFFF));

        int fill;
        int border;
        int textColor;
        if (!this.active) {
            fill = IrisTheme.bgA(0xA0);
            border = IrisTheme.border();
            textColor = IrisTheme.mutedA(0xAA);
        } else if (accented) {
            fill = hovered ? IrisTheme.accentFillHover() : IrisTheme.accentFill();
            border = IrisTheme.accent();
            textColor = 0xFFFFFFFF;
        } else {
            fill = hovered ? IrisTheme.bgHover() : IrisTheme.bgLight();
            border = hovered ? IrisTheme.accentDim() : IrisTheme.border();
            textColor = IrisTheme.text();
        }
        IrisTheme.roundRect(g, x, y, w, h, fill);
        IrisTheme.roundBorder(g, x, y, w, h, border);

        Font font = Minecraft.getInstance().font;
        String label = getMessage().getString();
        int room = w - 6 - inset;
        if (font.width(label) > room) {
            while (label.length() > 1 && font.width(label + "\u2026") > room) {
                label = label.substring(0, label.length() - 1);
            }
            label = label + "\u2026";
        }
        int tx = inset > 0 ? x + inset : x + (w - font.width(label)) / 2;
        g.drawString(font, label, tx, y + (h - 8) / 2, textColor);
    }

    public static Builder builder(Component message, Button.OnPress onPress) {
        return new Builder(message, onPress);
    }

    /** Mirrors the parts of {@code Button.Builder} the emote screens use. */
    public static final class Builder {
        private final Component message;
        private final Button.OnPress onPress;
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;
        private int inset;

        private Builder(Component message, Button.OnPress onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public Builder bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        /** Left-aligns the label with this much space on the left (room for an item icon). */
        public Builder inset(int inset) {
            this.inset = inset;
            return this;
        }

        public ThemedButton build() {
            ThemedButton b = new ThemedButton(x, y, width, height, message, onPress);
            b.inset = inset;
            return b;
        }
    }
}
