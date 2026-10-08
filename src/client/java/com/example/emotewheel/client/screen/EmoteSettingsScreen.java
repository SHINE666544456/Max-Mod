package com.example.emotewheel.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.example.emotewheel.client.EmoteConfig;
import com.example.emotewheel.client.LookTextures;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** All the options in one place. Colour fields take a hex code (e.g. E8A0B0) then Save. */
public class EmoteSettingsScreen extends Screen {
    private static final String[] PARTICLES = { "Off", "Light", "Full" };
    private static final String[] CAT_WHEN = { "Never", "During cat emotes", "Always (only you see it)" };
    private static final String[] EARS = { "Off", "Cat", "Fox", "Bunny", "Bear" };
    private static final String[] TAILS = { "Off", "Cat", "Fox", "Bunny" };
    private static final String[] PETS = { "Off", "Kitten", "Fox kit", "Bunny" };

    /** Mob-inspired colour presets: name, outer, inner, tail */
    private static final String[][] PRESETS = {
        { "Tabby",      "C47A2A", "F5C6A0", "C47A2A" },
        { "Black cat",  "1A1A1A", "3A3A3A", "1A1A1A" },
        { "White cat",  "E8E8E8", "F5D0D8", "E8E8E8" },
        { "Fox",        "D57E36", "E8B090", "E8DCC8" },
        { "Wolf",       "C8C0B0", "E8E0D0", "A09080" },
        { "Rabbit",     "9A6B4A", "E8C8A0", "9A6B4A" },
        { "Polar bear", "F0F0F0", "E8E8F0", "E0E0E0" },
        { "Ocelot",     "C9A66B", "F0D8A8", "C9A66B" },
        { "Mooshroom",  "A03020", "E08070", "803020" },
        { "Default",    "2C1A12", "E8A0B0", "2C1A12" },
    };

    private record Opt(Supplier<String> label, Runnable action) {}

    private final Screen parent;
    private final List<Opt> opts = new ArrayList<>();
    private final List<Button> buttons = new ArrayList<>();
    private EditBox outerBox, innerBox, tailBox;
    private String colourStatus = "";

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
        opts.add(new Opt(() -> "Multi-Shadow clones: " + c.multiCloneCount + " (click +1)", () -> {
            // No hard cap. Click adds 1. Edit config/emote_wheel.json for huge values; very high counts will lag.
            c.multiCloneCount = c.multiCloneCount + 1;
        }));

        int colW = 190, gap = 6, rowH = 24;
        int x0 = width / 2 - colW - gap / 2, x1 = width / 2 + gap / 2;
        int y0 = 36;
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

        int hy = y0 + (opts.size() / 2) * rowH + 28;
        int fieldW = 88;
        outerBox = hexField(x0 + 92, hy, fieldW, c.earOuter);
        innerBox = hexField(x1 + 92, hy, fieldW, c.earInner);
        tailBox = hexField(x0 + 92, hy + 24, fieldW, c.tailColor);
        addRenderableWidget(outerBox);
        addRenderableWidget(innerBox);
        addRenderableWidget(tailBox);
        addRenderableWidget(Button.builder(Component.literal("Save colours"), b -> saveColours())
            .bounds(x1, hy + 24, colW, 20).build());

        // Mob colour presets (two rows)
        int py = hy + 50;
        int presetW = 72, presetGap = 4;
        int totalW = PRESETS.length / 2 * (presetW + presetGap) - presetGap;
        int px0 = width / 2 - totalW / 2;
        for (int i = 0; i < PRESETS.length; i++) {
            final String[] p = PRESETS[i];
            int row = i / 5, col = i % 5;
            int px = px0 + col * (presetW + presetGap);
            int pyy = py + row * 22;
            addRenderableWidget(Button.builder(Component.literal(p[0]), b -> applyPreset(p))
                .bounds(px, pyy, presetW, 20).build());
        }

        int doneY = Math.min(height - 28, py + 50);
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(width / 2 - 60, doneY, 120, 20).build());
    }

    private EditBox hexField(int x, int y, int w, String value) {
        EditBox box = new EditBox(font, x, y, w, 18, Component.literal("hex"));
        box.setMaxLength(7);
        box.setValue(value == null ? "" : value);
        return box;
    }

    private void applyPreset(String[] p) {
        outerBox.setValue(p[1]);
        innerBox.setValue(p[2]);
        tailBox.setValue(p[3]);
        saveColours();
        colourStatus = "Preset: " + p[0];
    }

    private void saveColours() {
        EmoteConfig c = EmoteConfig.get();
        int o = LookTextures.parse(outerBox.getValue(), -1);
        int i = LookTextures.parse(innerBox.getValue(), -1);
        int t = LookTextures.parse(tailBox.getValue(), -1);
        if (o < 0 || i < 0 || t < 0) {
            colourStatus = "Use hex like 2C1A12 or #E8A0B0";
            return;
        }
        c.earOuter = LookTextures.format(o);
        c.earInner = LookTextures.format(i);
        c.tailColor = LookTextures.format(t);
        outerBox.setValue(c.earOuter);
        innerBox.setValue(c.earInner);
        tailBox.setValue(c.tailColor);
        c.save();
        LookTextures.rebuild();
        colourStatus = "Saved. Inner/outer ears + tail (with darker/lighter mix).";
    }

    private void refreshLabels() {
        for (int i = 0; i < opts.size(); i++) buttons.get(i).setMessage(Component.literal(opts.get(i).label().get()));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        String title = "Emote Settings";
        g.drawString(font, title, width / 2 - font.width(title) / 2, 14, 0xFFFFFFFF);
        int colW = 190, gap = 6;
        int x0 = width / 2 - colW - gap / 2, x1 = width / 2 + gap / 2;
        int hy = 36 + (opts.size() / 2) * 24 + 28;
        g.drawString(font, "Ear outer #", x0, hy + 5, 0xFFC8D0E8);
        g.drawString(font, "Ear inner #", x1, hy + 5, 0xFFC8D0E8);
        g.drawString(font, "Tail #", x0, hy + 29, 0xFFC8D0E8);
        if (outerBox != null) {
            int oc = 0xFF000000 | LookTextures.parse(outerBox.getValue(), LookTextures.OUTER);
            int ic = 0xFF000000 | LookTextures.parse(innerBox.getValue(), LookTextures.INNER);
            int tc = 0xFF000000 | LookTextures.parse(tailBox.getValue(), LookTextures.TAIL);
            g.fill(x0 - 16, hy, x0 - 4, hy + 18, oc);
            g.fill(x1 - 16, hy, x1 - 4, hy + 18, ic);
            g.fill(x0 - 16, hy + 24, x0 - 4, hy + 42, tc);
        }
        g.drawString(font, "Colour presets (mob-inspired)", width / 2 - font.width("Colour presets (mob-inspired)") / 2, hy + 52, 0xFF9AA4C0);
        if (!colourStatus.isEmpty()) {
            g.drawString(font, colourStatus, width / 2 - font.width(colourStatus) / 2, hy + 98, 0xFFA0E8B0);
        }
        String tip = "Pet uses its own fur atlas (not your hair). It lays down and sleeps on its own.";
        g.drawString(font, tip, width / 2 - font.width(tip) / 2, Math.min(height - 42, hy + 112), 0xFF9AA4C0);
    }

    @Override
    public void onClose() {
        EmoteConfig.get().save();
        minecraft.setScreen(parent);
    }
}
