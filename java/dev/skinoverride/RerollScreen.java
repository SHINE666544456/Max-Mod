package dev.skinoverride;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Pick the enchantment, minimum level and maximum price for the villager trade reroller, then start it. */
public class RerollScreen extends ThemedScreen {

    private static final int ROW_H = 13;

    private final Screen parent;
    private final ModConfig cfg = ModConfig.get();
    private final List<String> all = TradeReroller.knownEnchants();
    private List<String> shown = new ArrayList<>();
    private TextFieldWidget search;
    private double scroll;
    private String message = "";
    private long messageUntil;

    private int px, py, pw, ph, listW, listTop, listBottom, rx;

    public RerollScreen(Screen parent) {
        super(Text.literal("Trade reroller"));
        this.parent = parent;
    }

    private void say(String text) {
        message = text;
        messageUntil = System.currentTimeMillis() + 5000L;
    }

    private void filter() {
        String q = search == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        shown = new ArrayList<>();
        for (String id : all) {
            if (q.isEmpty() || id.contains(q)) {
                shown.add(id);
            }
        }
        scroll = 0;
    }

    @Override
    protected void build() {
        pw = Math.min(this.width - 20, 440);
        ph = this.height - 24 - 34;
        px = (this.width - pw) / 2;
        py = 24;
        listW = 150;
        listTop = py + 44;
        listBottom = py + ph - 4;
        rx = px + listW + 14;
        int rw = px + pw - 8 - rx;

        this.search = new TextFieldWidget(this.textRenderer, px + 4, py + 22, listW, 16, Text.literal("Search enchantments"));
        this.search.setPlaceholder(Text.literal("Search enchantment...").formatted(Formatting.DARK_GRAY));
        this.search.setMaxLength(30);
        this.search.setChangedListener(t -> filter());
        this.addDrawableChild(this.search);
        filter();

        int y = py + 58;
        int half = (rw - 4) / 2;
        button(rx, y, half, 16, () -> "Min level: " + cfg.rerollMinLevel, () -> true, false, () -> {
            cfg.rerollMinLevel = cfg.rerollMinLevel % 5 + 1;
            cfg.save();
        });
        button(rx + half + 4, y, half, 16, () -> "Max tries: " + cfg.rerollMaxTries, () -> true, false, () -> {
            int[] steps = {50, 100, 300, 600, 1000};
            int next = steps[0];
            for (int i = 0; i < steps.length; i++) {
                if (steps[i] > cfg.rerollMaxTries) {
                    next = steps[i];
                    break;
                }
            }
            cfg.rerollMaxTries = next;
            cfg.save();
        });

        String[] methods = {"Auto", "Trade Cycling mod", "Break job block"};
        button(rx, py + 78, rw, 16, () -> "Method: " + methods[MathHelper.clamp(cfg.rerollMethod, 0, 2)],
                () -> true, false, () -> {
                    cfg.rerollMethod = (cfg.rerollMethod + 1) % 3;
                    cfg.save();
                });
        button(rx, py + 98, rw, 16, () -> "Trade Cycling key: " + (char) cfg.rerollCycleKey,
                () -> cfg.rerollMethod != 2, false, () -> {
                    int next = cfg.rerollCycleKey + 1;
                    cfg.rerollCycleKey = next > 90 || next < 65 ? 65 : next;
                    cfg.save();
                });

        int cy = py + 134;
        int bw = Math.max(26, (rw - 4 * 3) / 5);
        int[] deltas = {-8, -1, 1, 8};
        int bx = rx;
        for (int d : deltas) {
            int delta = d;
            button(bx, cy, bw, 16, () -> (delta > 0 ? "+" : "") + delta, () -> true, false, () -> {
                cfg.rerollMaxCost = MathHelper.clamp(cfg.rerollMaxCost + delta, 1, 64);
                cfg.save();
            });
            bx += bw + 3;
        }

        button(rx, py + 158, rw, 20,
                () -> TradeReroller.active() ? "Stop rerolling" : "Start rerolling", () -> true, true, () -> {
                    if (TradeReroller.active()) {
                        TradeReroller.stop(this.client, "Reroll stopped");
                        say("Stopped");
                        return;
                    }
                    String error = TradeReroller.start(this.client, null);
                    if (error != null) {
                        say(error);
                    } else {
                        this.client.setScreen(null);
                    }
                });

        int by = this.height - 28;
        button(px + (pw - 110) / 2, by, 110, 20, "Done", false, this::close);
    }

    private double maxScroll() {
        return Math.max(0, shown.size() * ROW_H + 4 - (listBottom - listTop));
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        Ui.panel(c, px, py, pw, ph);
        Ui.header(c, this.textRenderer, "Villager trade reroller", px + 1, py + 1, pw - 2);
        Ui.panel(c, px + 4, listTop, listW, listBottom - listTop);
        super.render(c, mouseX, mouseY, delta);

        scroll = MathHelper.clamp(scroll, 0.0, maxScroll());
        c.enableScissor(px + 5, listTop + 1, px + 3 + listW, listBottom - 1);
        String want = cfg.rerollEnchant == null ? "" : cfg.rerollEnchant.replace("minecraft:", "");
        for (int i = 0; i < shown.size(); i++) {
            int y = listTop + 3 + i * ROW_H - (int) scroll;
            if (y + ROW_H < listTop || y > listBottom) {
                continue;
            }
            String id = shown.get(i);
            boolean sel = id.equals(want);
            boolean hov = Ui.inside(mouseX, mouseY, px + 4, y, listW, ROW_H) && mouseY >= listTop && mouseY < listBottom;
            if (sel) {
                Ui.roundRect(c, px + 7, y, listW - 6, ROW_H, Ui.withAlpha(Ui.ACCENT, 0x80));
            } else if (hov) {
                Ui.roundRect(c, px + 7, y, listW - 6, ROW_H, 0x40FFFFFF);
            }
            Ui.text(c, this.textRenderer, TradeReroller.pretty(id), px + 11, y + 3, Ui.TEXT);
        }
        c.disableScissor();

        Ui.text(c, this.textRenderer, "Looking for", rx, py + 22, Ui.MUTED);
        Ui.text(c, this.textRenderer, TradeReroller.pretty(want) + " " + cfg.rerollMinLevel + "+", rx, py + 34, Ui.ACCENT);
        Ui.text(c, this.textRenderer, "for at most " + cfg.rerollMaxCost + " emeralds", rx, py + 46, Ui.TEXT);
        Ui.text(c, this.textRenderer, "Max price (emeralds, 1-64)", rx, py + 122, Ui.MUTED);

        VillagerEntity v = TradeReroller.lastVillager();
        int infoY = py + 184;
        boolean tc = TradeReroller.cyclingModLoaded();
        Ui.text(c, this.textRenderer, tc ? "Trade Cycling mod found: Auto uses its cycle key."
                : "Trade Cycling mod not found: Auto breaks the job block.", rx, infoY, tc ? Ui.GOOD : Ui.MUTED);
        Ui.text(c, this.textRenderer, "Stand within 3 blocks of an untraded villager.", rx, infoY + 11, Ui.MUTED);
        Ui.text(c, this.textRenderer, "Breaking needs the job block in your hotbar.", rx, infoY + 22, Ui.MUTED);
        Ui.text(c, this.textRenderer, "Many servers forbid automation. Check first.", rx, infoY + 33, Ui.WARN);
        if (v == null) {
            Ui.text(c, this.textRenderer, "Right-click a villager once, or look at one.", rx, infoY + 44, Ui.MUTED);
        }
        String line = System.currentTimeMillis() < messageUntil ? message : TradeReroller.status();
        if (line != null && !line.isEmpty()) {
            Ui.text(c, this.textRenderer, this.textRenderer.trimToWidth(line, px + pw - 8 - rx), rx, py + ph - 14, Ui.ACCENT);
        }

        drawButtons(c, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled) || clickButtons(click)) {
            return true;
        }
        if (click.button() != 0) {
            return false;
        }
        if (Ui.inside(click.x(), click.y(), px + 4, listTop, listW, listBottom - listTop)) {
            int index = (int) ((click.y() - listTop - 3 + scroll) / ROW_H);
            if (index >= 0 && index < shown.size()) {
                cfg.rerollEnchant = shown.get(index);
                cfg.save();
                clickSound();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = MathHelper.clamp(scroll - verticalAmount * ROW_H * 2, 0.0, maxScroll());
        return true;
    }

    @Override
    public void close() {
        cfg.save();
        this.client.setScreen(this.parent);
    }
}
