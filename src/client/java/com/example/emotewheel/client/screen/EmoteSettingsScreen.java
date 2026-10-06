package com.example.emotewheel.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.example.emotewheel.client.EmoteConfig;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** All the options in one place. Every button cycles through its choices; changes save immediately. */
public class EmoteSettingsScreen extends Screen {
    private static final String[] PARTICLES = { "Off", "Light", "Full" };
    private static final String[] CAT_WHEN = { "Never", "During cat emotes", "Always (only you see it)" };
    private static final String[] EARS = { "Off", "Cat", "Fox", "Bunny", "Bear" };
    private static final String[] TAILS = { "Off", "Cat", "Fox", "Bunny" };
    private static final String[] PETS = { "Off", "Kitten", "Fox kit", "Bunny" };

    private record Opt(Supplier<String> label, Runnable action) {}

    private final Screen parent;
    private final List<Opt> opts = new ArrayList<>();
    private final List<Button> buttons = new ArrayList<>();

    public EmoteSettingsScreen(Screen parent) {
        super(Component.literal("Emote Settings"));
        this.parent = parent;
    }

    private static String onOff(boolean b) { return b ? "ON" : "OFF"; }

    @Override
    protected void init() {
        EmoteConfig c = EmoteConfig.get();
        opts.clear();
        buttons.clear();
        opts.add(new Opt(() -> "Stop emote when I move: " + onOff(c.cancelOnMove), () -> c.cancelOnMove = !c.cancelOnMove));
        opts.add(new Opt(() -> "Third person while emoting: " + onOff(c.thirdPerson), () -> c.thirdPerson = !c.thirdPerson));
        opts.add(new Opt(() -> "See other players' emotes: " + onOff(c.showOthers), () -> c.showOthers = !c.showOthers));
        opts.add(new Opt(() -> "Particles: " + PARTICLES[c.fxMode], () -> c.fxMode = (c.fxMode + 1) % PARTICLES.length));
        opts.add(new Opt(() -> "Show cat parts: " + CAT_WHEN[c.catMode], () -> c.catMode = (c.catMode + 1) % CAT_WHEN.length));
        opts.add(new Opt(() -> "Ears: " + EARS[c.earStyle], () -> c.earStyle = (c.earStyle + 1) % EARS.length));
        opts.add(new Opt(() -> "Tail: " + TAILS[c.tailStyle], () -> c.tailStyle = (c.tailStyle + 1) % TAILS.length));
        opts.add(new Opt(() -> "Pet on my head: " + PETS[c.pet], () -> c.pet = (c.pet + 1) % PETS.length));

        int colW = 190, gap = 6, rowH = 24;
        int x0 = width / 2 - colW - gap / 2, x1 = width / 2 + gap / 2;
        int y0 = 40;
        for (int i = 0; i < opts.size(); i++) {
            final Opt o = opts.get(i);
            int x = (i % 2 == 0) ? x0 : x1, y = y0 + (i / 2) * rowH;
            Button b = Button.builder(Component.literal(o.label().get()), btn -> {
                o.action().run();
                c.save();
                refreshLabels();
            }).bounds(x, y, colW, 20).build();
            buttons.add(b);
            addRenderableWidget(b);
        }
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(width / 2 - 60, Math.min(height - 28, y0 + (opts.size() / 2) * rowH + 40), 120, 20).build());
    }

    private void refreshLabels() {
        for (int i = 0; i < opts.size(); i++) buttons.get(i).setMessage(Component.literal(opts.get(i).label().get()));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        String title = "Emote Settings";
        g.drawString(font, title, width / 2 - font.width(title) / 2, 16, 0xFFFFFFFF);
        String tip = "Ears, tail and pet are textured from your skin (hair colour on top, skin tone underneath).";
        g.drawString(font, tip, width / 2 - font.width(tip) / 2, 40 + (opts.size() / 2) * 24 + 8, 0xFF9AA4C0);
        String tip2 = "Open the Emote Menu and hover a cat emote to preview your look.";
        g.drawString(font, tip2, width / 2 - font.width(tip2) / 2, 40 + (opts.size() / 2) * 24 + 20, 0xFF9AA4C0);
    }

    @Override
    public void onClose() {
        EmoteConfig.get().save();
        minecraft.setScreen(parent);
    }
}
