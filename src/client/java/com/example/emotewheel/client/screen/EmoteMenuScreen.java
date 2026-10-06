package com.example.emotewheel.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.emotewheel.client.EmoteAccess;
import com.example.emotewheel.client.EmoteClient;
import com.example.emotewheel.client.EmoteConfig;
import com.example.emotewheel.client.Icons;
import com.example.emotewheel.emote.Category;
import com.example.emotewheel.emote.Emote;
import com.example.emotewheel.emote.Emotes;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

/**
 * The full emote library and wheel editor, with a live preview of your character.
 * Left: every emote (search, category filter, favorites). Middle: preview (hover an emote to watch it).
 * Right: the three wheel pages. Pick a wheel slot, then click emotes to fill it.
 */
public class EmoteMenuScreen extends Screen {
    private static final int SLOTS = EmoteConfig.SLOTS;
    private static final int MARGIN = 10;

    private final Screen parent;
    private int selSlot;
    private int page;
    private int filterIdx = 0; // 0 = all, 1 = favorites, 2.. = categories, last = recent
    private String query = "";
    private Emote selected;
    private int listPage = 0;
    private int listPages = 1;

    private EditBox search;
    private Button filterBtn, prevBtn, nextBtn, playBtn, favBtn, clearBtn, randomBtn;
    private Button settingsBtn;
    private final List<Button> grid = new ArrayList<>();
    private Emote[] gridEmote = new Emote[0];
    private final Button[] slotBtns = new Button[SLOTS];
    private final Button[] pageBtns = new Button[EmoteConfig.PAGES];
    private int cols, rows, perPage, rx, leftW, navY;
    private int previewX, previewW;

    public EmoteMenuScreen(Screen parent, int slot) {
        super(Component.literal("Emotes"));
        this.parent = parent;
        this.selSlot = slot;
        this.page = EmoteConfig.get().page;
        this.listPage = Math.max(0, EmoteConfig.get().listPage);
        this.filterIdx = Math.max(0, EmoteConfig.get().filterIdx);
        if (this.filterIdx >= filterCount()) this.filterIdx = 0;
    }

    @Override
    protected void init() {
        grid.clear();
        int rightW = 176;
        previewW = width >= 640 ? 110 : 0;
        leftW = width - rightW - previewW - (previewW > 0 ? 4 : 3) * MARGIN;
        if (leftW < 190 && previewW > 0) { previewW = 0; leftW = width - rightW - 3 * MARGIN; }
        leftW = Math.max(150, leftW);
        int lx = MARGIN;
        previewX = lx + leftW + MARGIN;
        rx = previewX + (previewW > 0 ? previewW + MARGIN : 0);

        // search + filter
        search = new EditBox(font, lx, 22, leftW - 98, 18, Component.literal("Search"));
        search.setHint(Component.literal("Search emotes..."));
        search.setValue(query);
        search.setResponder(s -> { query = s.toLowerCase(Locale.ROOT); listPage = 0; persistMenuPage(); refresh(); });
        addRenderableWidget(search);

        filterBtn = Button.builder(Component.literal("All"), b -> {
            filterIdx = (filterIdx + 1) % filterCount();
            listPage = 0;
            persistMenuPage();
            refresh();
        }).bounds(lx + leftW - 94, 21, 94, 20).build();
        addRenderableWidget(filterBtn);

        // emote grid
        cols = Math.max(2, leftW / 96);
        int cellW = (leftW - (cols - 1) * 4) / cols;
        int top = 46;
        rows = Math.max(3, Math.min(10, (height - top - 52) / 22));
        perPage = cols * rows;
        gridEmote = new Emote[perPage];
        for (int i = 0; i < perPage; i++) {
            final int idx = i;
            int r = i / cols, c = i % cols;
            Button b = Button.builder(Component.empty(), btn -> onGridClick(idx))
                .bounds(lx + c * (cellW + 4), top + r * 22, cellW, 20).build();
            grid.add(b);
            addRenderableWidget(b);
        }

        navY = top + rows * 22 + 4;
        prevBtn = Button.builder(Component.literal("<"), b -> { listPage--; persistMenuPage(); refresh(); })
            .bounds(lx, navY, 24, 18).build();
        nextBtn = Button.builder(Component.literal(">"), b -> { listPage++; persistMenuPage(); refresh(); })
            .bounds(lx + leftW - 24, navY, 24, 18).build();
        addRenderableWidget(prevBtn);
        addRenderableWidget(nextBtn);

        // wheel editor (right side)
        int pw = (rightW - 8) / 3;
        for (int p = 0; p < EmoteConfig.PAGES; p++) {
            final int pp = p;
            pageBtns[p] = Button.builder(Component.literal("Page " + (p + 1)), b -> {
                page = pp;
                EmoteConfig cfg = EmoteConfig.get();
                cfg.page = pp;
                cfg.save();
                refresh();
            }).bounds(rx + p * (pw + 4), 22, pw, 18).build();
            addRenderableWidget(pageBtns[p]);
        }
        int sw = (rightW - 4) / 2;
        for (int s = 0; s < SLOTS; s++) {
            final int ss = s;
            slotBtns[s] = Button.builder(Component.empty(), b -> { selSlot = (selSlot == ss) ? -1 : ss; refresh(); })
                .bounds(rx + (s % 2) * (sw + 4), 44 + (s / 2) * 22, sw, 20).build();
            addRenderableWidget(slotBtns[s]);
        }

        playBtn = Button.builder(Component.literal("\u25B6 Play"), b -> {
            if (selected != null) { minecraft.setScreen(null); EmoteClient.playLocal(selected); }
        }).bounds(rx, 158, sw, 20).build();
        favBtn = Button.builder(Component.literal("\u2605 Favorite"), b -> {
            if (selected != null) { EmoteConfig.get().toggleFavorite(selected.id()); refresh(); }
        }).bounds(rx + sw + 4, 158, sw, 20).build();
        clearBtn = Button.builder(Component.literal("Clear Slot"), b -> {
            if (selSlot >= 0) { EmoteConfig.get().setSlot(page, selSlot, null); refresh(); }
        }).bounds(rx, 180, sw, 20).build();
        randomBtn = Button.builder(Component.literal("Random"), b -> {
            List<Emote> pool = filtered();                         // respects the current tab/search
            if (pool.isEmpty()) pool = EmoteAccess.visible();
            if (pool.isEmpty()) return;
            Emote pick = pool.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(pool.size()));
            minecraft.setScreen(null);
            EmoteClient.playLocal(pick);
        }).bounds(rx + sw + 4, 180, sw, 20).build();
        addRenderableWidget(playBtn);
        addRenderableWidget(favBtn);
        addRenderableWidget(clearBtn);
        addRenderableWidget(randomBtn);

        // the options live in their own screen now; just two buttons along the bottom
        int bw = (width - 2 * MARGIN - 4) / 2;
        settingsBtn = Button.builder(Component.literal("Settings..."), b -> minecraft.setScreen(new EmoteSettingsScreen(this)))
            .bounds(MARGIN, height - 24, bw, 20).build();
        addRenderableWidget(settingsBtn);
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(MARGIN + bw + 4, height - 24, bw, 20).build());

        refresh();
    }

    // ---------------------------------------------------------------- logic

    private void persistMenuPage() {
        EmoteConfig cfg = EmoteConfig.get();
        cfg.listPage = Math.max(0, listPage);
        cfg.filterIdx = Math.max(0, filterIdx);
        cfg.save();
    }

    /** All, Favorites, one tab per category, then Recent. */
    private static int filterCount() { return 3 + Category.values().length; }
    private static int recentIdx() { return 2 + Category.values().length; }

    /** Search matches the name, the id, or the category ("anime", "dances"...). */
    private boolean matches(Emote e) {
        if (query.isEmpty()) return true;
        return e.name().toLowerCase(Locale.ROOT).contains(query) || e.id().contains(query)
            || e.category().label.toLowerCase(Locale.ROOT).contains(query);
    }

    private List<Emote> filtered() {
        EmoteConfig cfg = EmoteConfig.get();
        List<Emote> list = new ArrayList<>();
        if (filterIdx == recentIdx()) { // newest first
            for (String id : cfg.recent) {
                Emote e = Emotes.byId(id);
                if (e != null && EmoteAccess.canUse(e) && matches(e)) list.add(e);
            }
            return list;
        }
        for (Emote e : EmoteAccess.visible()) {
            if (filterIdx == 1 && !cfg.isFavorite(e.id())) continue;
            if (filterIdx >= 2 && e.category() != Category.values()[filterIdx - 2]) continue;
            if (!matches(e)) continue;
            list.add(e);
        }
        return list;
    }

    private void refresh() {
        EmoteConfig cfg = EmoteConfig.get();
        List<Emote> list = filtered();
        listPages = Math.max(1, (list.size() + perPage - 1) / perPage);
        listPage = Math.max(0, Math.min(listPage, listPages - 1));

        for (int i = 0; i < perPage; i++) {
            int k = listPage * perPage + i;
            Button b = grid.get(i);
            if (k < list.size()) {
                Emote e = list.get(k);
                gridEmote[i] = e;
                b.visible = true;
                b.setMessage(gridLabel(e, b.getWidth() - 24));
            } else {
                gridEmote[i] = null;
                b.visible = false;
            }
        }
        prevBtn.active = listPage > 0;
        nextBtn.active = listPage < listPages - 1;
        filterBtn.setMessage(Component.literal(filterName()));

        for (int p = 0; p < pageBtns.length; p++) pageBtns[p].active = (p != page);
        for (int s = 0; s < SLOTS; s++) {
            Emote e = slotEmote(s);
            Component label = Component.literal((s + 1) + ": " + (e == null ? "\u2014" : trim(e.name(), slotBtns[s].getWidth() - 30)));
            slotBtns[s].setMessage(s == selSlot ? Component.literal("").append(label).withStyle(ChatFormatting.YELLOW) : label);
        }
        playBtn.active = selected != null;
        favBtn.active = selected != null;
        clearBtn.active = selSlot >= 0 && cfg.slotId(page, selSlot) != null;

    }

    private Emote slotEmote(int s) {
        Emote e = Emotes.byId(EmoteConfig.get().slotId(page, s));
        return EmoteAccess.canUse(e) ? e : null;
    }

    private String filterName() {
        if (filterIdx == 0) return "All";
        if (filterIdx == 1) return "\u2605 Favorites";
        if (filterIdx == recentIdx()) return "Recent";
        return Category.values()[filterIdx - 2].label;
    }

    private Component gridLabel(Emote e, int maxWidth) {
        String s = (EmoteConfig.get().isFavorite(e.id()) ? "\u2605 " : "") + e.name();
        Component c = Component.literal(trim(s, maxWidth));
        return e == selected ? Component.literal("").append(c).withStyle(ChatFormatting.YELLOW) : c;
    }

    private void onGridClick(int idx) {
        Emote e = gridEmote[idx];
        if (e == null) return;
        selected = e;
        if (selSlot >= 0) {
            EmoteConfig.get().setSlot(page, selSlot, e.id());
            selSlot = (selSlot < SLOTS - 1) ? selSlot + 1 : -1; // move on to the next slot
        }
        refresh();
    }

    private String trim(String s, int max) {
        if (font.width(s) <= max) return s;
        while (s.length() > 1 && font.width(s + "…") > max) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);

        g.drawString(font, "Emote Menu  (" + EmoteAccess.visible().size() + " emotes)", MARGIN, 8, 0xFFFFFFFF);
        g.drawString(font, "Your wheel", rx, 8, 0xFFFFFFFF);

        // which emote does the preview show? the one under the mouse, else the selected one
        Emote hover = null;
        for (int i = 0; i < perPage; i++) {
            Button b = grid.get(i);
            if (b.visible && gridEmote[i] != null && mouseX >= b.getX() && mouseX < b.getX() + b.getWidth()
                && mouseY >= b.getY() && mouseY < b.getY() + b.getHeight()) {
                hover = gridEmote[i];
                break;
            }
            if (b.visible && gridEmote[i] != null) g.renderItem(Icons.of(gridEmote[i]), b.getX() + 3, b.getY() + 2);
        }
        for (int s = 0; s < SLOTS; s++) {
            Emote e = slotEmote(s);
            if (e != null) g.renderItem(Icons.of(e), slotBtns[s].getX() + 2, slotBtns[s].getY() + 2);
        }
        Emote previewing = hover != null ? hover : selected;

        String pg = (listPage + 1) + " / " + listPages;
        g.drawString(font, pg, MARGIN + leftW / 2 - font.width(pg) / 2, navY + 5, 0xFFC8D0E8);

        // live preview of your character performing the emote
        if (previewW > 0 && minecraft.player != null) {
            int x1 = previewX, y1 = 44, x2 = previewX + previewW, y2 = navY + 14;
            g.fill(x1, y1, x2, y2, 0x66000000);
            EmoteClient.setPreview(previewing);
            EmoteClient.beginPreview();
            try {
                InventoryScreen.renderEntityInInventoryFollowsMouse(g, x1, y1, x2, y2, Math.max(30, (y2 - y1) / 3),
                    0.0625f, mouseX, mouseY, minecraft.player);
            } finally {
                EmoteClient.endPreview();
            }
            String label = previewing == null ? "Hover an emote" : trim(previewing.name(), previewW - 4);
            g.drawString(font, label, x1 + previewW / 2 - font.width(label) / 2, y2 - 12, 0xFFFFFFFF);
        } else {
            EmoteClient.setPreview(previewing);
        }

        // selection info
        int y = 134;
        if (selSlot >= 0) {
            g.drawString(font, "Filling slot " + (selSlot + 1) + " - click emotes", rx, y, 0xFFFFE070);
        } else {
            g.drawString(font, "Pick a slot, then click emotes", rx, y, 0xFF9AA4C0);
        }
        if (selected != null) {
            g.drawString(font, trim(selected.name() + "  (" + selected.category().label + ")", 176), rx, y + 12, 0xFFFFFFFF);
        } else {
            g.drawString(font, "Click an emote to select it", rx, y + 12, 0xFF9AA4C0);
        }
        g.drawString(font, "Keys: Options > Controls > Key Binds", rx, 204, 0xFF707890);
    }

    @Override
    public void onClose() {
        persistMenuPage();
        EmoteConfig.get().save();
        EmoteClient.setPreview(null);
        minecraft.setScreen(parent);
    }
}
