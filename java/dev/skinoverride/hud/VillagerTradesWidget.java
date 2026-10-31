package dev.skinoverride.hud;

import dev.skinoverride.ModConfig;
import dev.skinoverride.Ui;
import dev.skinoverride.VillagerTrades;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.passive.VillagerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Appears while you look at a villager and lists the enchanted book trades it has. The game only tells you a
 * villager's trades when you open them, so open each villager once; after that this panel remembers them.
 */
public class VillagerTradesWidget implements HudWidget {

    private static final int LINE = 10;

    private final List<String> lines = new ArrayList<>();
    private final List<Integer> colors = new ArrayList<>();
    private int w;
    private int h;

    @Override
    public String id() {
        return "villager_trades";
    }

    @Override
    public String label() {
        return "Villager trades";
    }

    @Override
    public float defaultX() {
        return 0.5f;
    }

    @Override
    public float defaultY() {
        return 0.62f;
    }

    @Override
    public boolean enabled() {
        return ModConfig.get().villagerHudOn;
    }

    @Override
    public boolean visible(MinecraftClient mc) {
        return mc.currentScreen == null && mc.targetedEntity instanceof VillagerEntity;
    }

    private void add(String text, int color) {
        lines.add(text);
        colors.add(color);
    }

    @Override
    public void prepare(MinecraftClient mc, boolean preview) {
        lines.clear();
        colors.clear();
        add("Villager trades", Ui.ACCENT);
        if (preview || !(mc.targetedEntity instanceof VillagerEntity v)) {
            add("Mending I - 12 emeralds", Ui.TEXT);
            add("Unbreaking III - 28 emeralds", Ui.TEXT);
            add("+ 4 other trades", Ui.MUTED);
        } else {
            VillagerTrades.Info info = VillagerTrades.get(v.getUuid());
            if (info == null) {
                add("Open its trades once", Ui.MUTED);
                add("to see its books here.", Ui.MUTED);
            } else {
                if (info.books().isEmpty()) {
                    add("No enchanted books", Ui.MUTED);
                }
                int shown = 0;
                for (String line : info.books()) {
                    if (shown++ >= 6) {
                        add("...", Ui.MUTED);
                        break;
                    }
                    add(line, Ui.TEXT);
                }
                if (info.others() > 0) {
                    add("+ " + info.others() + " other trade" + (info.others() == 1 ? "" : "s"), Ui.MUTED);
                }
            }
        }
        MinecraftClient client = MinecraftClient.getInstance();
        int max = 0;
        for (String line : lines) {
            max = Math.max(max, client.textRenderer.getWidth(line));
        }
        w = max + 10;
        h = lines.size() * LINE + 6;
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
        Ui.roundRect(c, x - 1, y - 1, w + 2, h + 2, Ui.BG);
        Ui.roundBorder(c, x - 1, y - 1, w + 2, h + 2, Ui.BORDER);
        c.fill(x - 1, y - 1, x + 11, y, Ui.ACCENT);
        c.fill(x - 1, y - 1, x, y + 11, Ui.ACCENT);
        for (int i = 0; i < lines.size(); i++) {
            c.drawTextWithShadow(mc.textRenderer, lines.get(i), x + 5, y + 4 + i * LINE, colors.get(i));
        }
    }
}
