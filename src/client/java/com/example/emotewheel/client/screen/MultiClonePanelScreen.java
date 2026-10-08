package com.example.emotewheel.client.screen;

import com.example.emotewheel.client.EmoteAccess;
import com.example.emotewheel.client.EmoteConfig;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Tiny non-pausing panel for the owner only: change Multi-Shadow clone count without digging through settings.
 * Open/close with the "Multi-Shadow Clone Panel" keybind.
 */
public class MultiClonePanelScreen extends Screen {
    private EditBox countBox;
    private String status = "";

    public MultiClonePanelScreen() {
        super(Component.literal("Multi-Shadow Clones"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        if (!EmoteAccess.isOwner()) {
            onClose();
            return;
        }
        EmoteConfig c = EmoteConfig.get();
        int panelW = 220;
        int x = 12;
        int y = 12;

        countBox = new EditBox(font, x + 70, y + 28, 56, 18, Component.literal("count"));
        countBox.setMaxLength(6);
        countBox.setValue(String.valueOf(c.multiCloneCount));
        countBox.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        addRenderableWidget(countBox);

        addRenderableWidget(Button.builder(Component.literal("-10"), b -> nudge(-10))
            .bounds(x, y + 28, 32, 20).build());
        addRenderableWidget(Button.builder(Component.literal("-1"), b -> nudge(-1))
            .bounds(x + 34, y + 28, 28, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+1"), b -> nudge(1))
            .bounds(x + 130, y + 28, 28, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+10"), b -> nudge(10))
            .bounds(x + 160, y + 28, 32, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> apply())
            .bounds(x, y + 54, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Hide"), b -> onClose())
            .bounds(x + 74, y + 54, 70, 20).build());
    }

    private void nudge(int d) {
        EmoteConfig c = EmoteConfig.get();
        c.multiCloneCount = Math.max(1, c.multiCloneCount + d);
        c.save();
        if (countBox != null) countBox.setValue(String.valueOf(c.multiCloneCount));
        status = "Set to " + c.multiCloneCount + " — re-cast Multi-Shadow if active";
    }

    private void apply() {
        try {
            int n = Integer.parseInt(countBox.getValue().trim());
            if (n < 1) {
                status = "Must be at least 1";
                return;
            }
            EmoteConfig c = EmoteConfig.get();
            c.multiCloneCount = n;
            c.save();
            countBox.setValue(String.valueOf(n));
            status = "Set to " + n + " — re-cast Multi-Shadow if active";
        } catch (NumberFormatException e) {
            status = "Type a whole number";
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        // dim only a small card, leave the world visible
        int x = 8, y = 8, w = 228, h = 88;
        g.fill(x, y, x + w, y + h, 0xC0101018);
        g.renderOutline(x, y, w, h, 0xFFE8C060);
        g.drawString(font, "Multi-Shadow Clones", x + 8, y + 8, 0xFFE8C060);
        g.drawString(font, "Count", x + 8, y + 32, 0xFFC8D0E8);
        if (!status.isEmpty()) {
            g.drawString(font, status, x + 8, y + 78, 0xFFA0E8B0);
        }
        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (countBox != null) {
            try {
                int n = Integer.parseInt(countBox.getValue().trim());
                if (n >= 1) {
                    EmoteConfig c = EmoteConfig.get();
                    c.multiCloneCount = n;
                    c.save();
                }
            } catch (NumberFormatException ignored) {}
        }
        minecraft.setScreen(null);
    }
}
