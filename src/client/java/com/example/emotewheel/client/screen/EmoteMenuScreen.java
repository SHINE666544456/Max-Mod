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
 * Full emote library + wheel editor with live preview.
 * Left: library (search / filter / grid). Center: character preview. Right: wheel slots.
 */
public class EmoteMenuScreen extends Screen {
    private static final int SLOTS = EmoteConfig.SLOTS;
    private static final int MARGIN = 12;

    // palette
    private static final int BG_PANEL = 0xD012141C;
    private static final int BG_PANEL_TOP = 0xE01A1E2A;
    private static final int BORDER = 0xFF3A4258;
    private static final int ACCENT = 0xFFE8C060;
    private static final int TEXT = 0xFFE8ECF4;
    private static final int TEXT_DIM = 0xFF9AA4C0;
    private static final int SELECT = 0x55E8C060;
    private static final int HOVER = 0x33FFFFFF;
    private static final int SLOT_SEL = 0x66E8C060;

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
    private int previewX, previewW, rightW;
    private int leftX, leftTop, cellW, cellH, gridTop;

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
        rightW = 180;
        previewW = width >= 700 ? 130 : (width >= 580 ? 100 : 0);
        leftW = width - rightW - previewW - (previewW > 0 ? 4 : 3) * MARGIN;
        if (leftW < 200 && previewW > 0) {
            previewW = 0;
            leftW = width - rightW - 3 * MARGIN;
        }
        leftW = Math.max(160, leftW);
        leftX = MARGIN;
        previewX = leftX + leftW + MARGIN;
        rx = previewX + (previewW > 0 ? previewW + MARGIN : 0);

        search = new EditBox(font, leftX + 8, 36, leftW - 118, 18, Component.literal("Search"));
        search.setHint(Component.literal("Search emotes..."));
        search.setValue(query);
        search.setResponder(s -> {
            query = s.toLowerCase(Locale.ROOT);
            listPage = 0;
            persistMenuPage();
            refresh();
        });
        addRenderableWidget(search);

        filterBtn = Button.builder(Component.literal("All"), b -> {
            filterIdx = (filterIdx + 1) % filterCount();
            listPage = 0;
            persistMenuPage();
            refresh();
        }).bounds(leftX + leftW - 102, 35, 94, 20).build();
        addRenderableWidget(filterBtn);

        cols = Math.max(2, leftW / 100);
        cellW = (leftW - 16 - (cols - 1) * 4) / cols;
        cellH = 22;
        gridTop = 62;
        rows = Math.max(3, Math.min(11, (height - gridTop - 56) / (cellH + 3)));
        perPage = cols * rows;
        gridEmote = new Emote[perPage];
        for (int i = 0; i < perPage; i++) {
            final int idx = i;
            int r = i / cols, c = i % cols;
            Button b = Button.builder(Component.empty(), btn -> onGridClick(idx))
                .bounds(leftX + 8 + c * (cellW + 4), gridTop + r * (cellH + 3), cellW, cellH).build();
            grid.add(b);
            addRenderableWidget(b);
        }

        navY = gridTop + rows * (cellH + 3) + 6;
        prevBtn = Button.builder(Component.literal("<"), b -> {
            listPage--;
            persistMenuPage();
            refresh();
        }).bounds(leftX + 8, navY, 28, 18).build();
        nextBtn = Button.builder(Component.literal(">"), b -> {
            listPage++;
            persistMenuPage();
            refresh();
        }).bounds(leftX + leftW - 36, navY, 28, 18).build();
        addRenderableWidget(prevBtn);
        addRenderableWidget(nextBtn);

        int pw = (rightW - 12) / 3;
        for (int p = 0; p < EmoteConfig.PAGES; p++) {
            final int pp = p;
            pageBtns[p] = Button.builder(Component.literal("P" + (p + 1)), b -> {
                page = pp;
                EmoteConfig cfg = EmoteConfig.get();
                cfg.page = pp;
                cfg.save();
                refresh();
            }).bounds(rx + 6 + p * (pw + 4), 36, pw, 18).build();
            addRenderableWidget(pageBtns[p]);
        }
        int sw = (rightW - 12) / 2;
        for (int s = 0; s < SLOTS; s++) {
            final int ss = s;
            slotBtns[s] = Button.builder(Component.empty(), b -> {
                selSlot = (selSlot == ss) ? -1 : ss;
                refresh();
            }).bounds(rx + 6 + (s % 2) * (sw + 4), 60 + (s / 2) * 22, sw, 20).build();
            addRenderableWidget(slotBtns[s]);
        }

        playBtn = Button.builder(Component.literal("\u25B6 Play"), b -> {
            if (selected != null) {
                minecraft.setScreen(null);
                EmoteClient.playLocal(selected);
            }
        }).bounds(rx + 6, height - 78, sw, 20).build();
        favBtn = Button.builder(Component.literal("\u2605 Fav"), b -> {
            if (selected != null) {
                EmoteConfig.get().toggleFavorite(selected.id());
                refresh();
            }
        }).bounds(rx + 6 + sw + 4, height - 78, sw, 20).build();
        clearBtn = Button.builder(Component.literal("Clear Slot"), b -> {
            if (selSlot >= 0) {
                EmoteConfig.get().setSlot(page, selSlot, null);
                refresh();
            }
        }).bounds(rx + 6, height - 54, sw, 20).build();
        randomBtn = Button.builder(Component.literal("Random"), b -> {
            List<Emote> pool = filtered();
            if (pool.isEmpty()) pool = EmoteAccess.visible();
            if (pool.isEmpty()) return;
            Emote pick = pool.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(pool.size()));
            selected = pick;
            if (selSlot >= 0) {
                EmoteConfig.get().setSlot(page, selSlot, pick.id());
                selSlot = (selSlot < SLOTS - 1) ? selSlot + 1 : -1;
            }
            refresh();
        }).bounds(rx + 6 + sw + 4, height - 54, sw, 20).build();
        settingsBtn = Button.builder(Component.literal("Settings"), b ->
            minecraft.setScreen(new EmoteSettingsScreen(this))).bounds(rx + 6, height - 30, rightW - 12, 18).build();

        addRenderableWidget(playBtn);
        addRenderableWidget(favBtn);
        addRenderableWidget(clearBtn);
        addRenderableWidget(randomBtn);
        addRenderableWidget(settingsBtn);

        leftTop = 22;
        refresh();
    }

    private void persistMenuPage() {
        EmoteConfig cfg = EmoteConfig.get();
        cfg.listPage = listPage;
        cfg.filterIdx = filterIdx;
        cfg.save();
    }

    private int filterCount() {
        return 2 + Category.values().length + 1;
    }

    private int recentIdx() {
        return filterCount() - 1;
    }

    private boolean matches(Emote e) {
        if (query == null || query.isEmpty()) return true;
        return e.name().toLowerCase(Locale.ROOT).contains(query)
            || e.id().contains(query)
            || e.category().label.toLowerCase(Locale.ROOT).contains(query);
    }

    private List<Emote> filtered() {
        EmoteConfig cfg = EmoteConfig.get();
        List<Emote> list = new ArrayList<>();
        if (filterIdx == recentIdx()) {
            for (String id : cfg.recent) {
                Emote e = Emotes.byId(id);
                if (e != null && EmoteAccess.canUse(e) && matches(e)) list.add(e);
            }
            return list;
        }
        for (Emote e : EmoteAccess.visible()) {
            if (filterIdx == 1 && !cfg.isFavorite(e.id())) continue;
            if (filterIdx >= 2 && filterIdx < recentIdx() && e.category() != Category.values()[filterIdx - 2]) continue;
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
            slotBtns[s].setMessage(s == selSlot
                ? Component.literal("").append(label).withStyle(ChatFormatting.YELLOW)
                : label);
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
            selSlot = (selSlot < SLOTS - 1) ? selSlot + 1 : -1;
        }
        refresh();
    }

    private String trim(String s, int max) {
        if (font.width(s) <= max) return s;
        while (s.length() > 1 && font.width(s + "\u2026") > max) s = s.substring(0, s.length() - 1);
        return s + "\u2026";
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, BG_PANEL);
        g.fill(x, y, x + w, y + 16, BG_PANEL_TOP);
        g.renderOutline(x, y, w, h, BORDER);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0x88000000);

        int leftH = navY + 28 - leftTop;
        panel(g, leftX, leftTop, leftW, leftH);
        g.drawString(font, "Library", leftX + 8, leftTop + 4, ACCENT);

        if (previewW > 0) {
            int prevH = navY + 14 - 22;
            panel(g, previewX, 22, previewW, prevH);
            g.drawString(font, "Preview", previewX + 8, 26, ACCENT);
        }

        int panelRightW = width - rx - MARGIN;
        panel(g, rx, 22, panelRightW, height - 22 - MARGIN);
        g.drawString(font, "Wheel", rx + 8, 26, ACCENT);

        super.render(g, mouseX, mouseY, delta);

        String title = "IRIS Emote Menu  \u00B7  " + EmoteAccess.visible().size() + " emotes";
        g.drawString(font, title, MARGIN, 8, TEXT);

        Emote hover = null;
        for (int i = 0; i < grid.size(); i++) {
            Button b = grid.get(i);
            if (!b.visible || gridEmote[i] == null) continue;
            boolean isSel = gridEmote[i] == selected;
            boolean isHover = mouseX >= b.getX() && mouseX < b.getX() + b.getWidth()
                && mouseY >= b.getY() && mouseY < b.getY() + b.getHeight();
            if (isHover) hover = gridEmote[i];
            if (isSel) g.fill(b.getX(), b.getY(), b.getX() + b.getWidth(), b.getY() + b.getHeight(), SELECT);
            else if (isHover) g.fill(b.getX(), b.getY(), b.getX() + b.getWidth(), b.getY() + b.getHeight(), HOVER);
            g.renderItem(Icons.of(gridEmote[i]), b.getX() + 3, b.getY() + 3);
        }

        for (int s = 0; s < SLOTS; s++) {
            Button b = slotBtns[s];
            if (s == selSlot) {
                g.fill(b.getX() - 1, b.getY() - 1, b.getX() + b.getWidth() + 1, b.getY() + b.getHeight() + 1, SLOT_SEL);
                g.renderOutline(b.getX() - 1, b.getY() - 1, b.getWidth() + 2, b.getHeight() + 2, ACCENT);
            }
            Emote e = slotEmote(s);
            if (e != null) g.renderItem(Icons.of(e), b.getX() + 2, b.getY() + 2);
        }

        for (int p = 0; p < pageBtns.length; p++) {
            if (p == page) {
                Button b = pageBtns[p];
                g.renderOutline(b.getX() - 1, b.getY() - 1, b.getWidth() + 2, b.getHeight() + 2, ACCENT);
            }
        }

        Emote previewing = hover != null ? hover : selected;

        String pg = (listPage + 1) + " / " + listPages;
        g.drawString(font, pg, leftX + leftW / 2 - font.width(pg) / 2, navY + 5, TEXT_DIM);

        if (previewW > 0 && minecraft.player != null) {
            int x1 = previewX + 4, y1 = 44, x2 = previewX + previewW - 4, y2 = navY + 10;
            g.fill(x1, y1, x2, y2, 0x66000000);
            EmoteClient.setPreview(previewing);
            EmoteClient.beginPreview();
            try {
                InventoryScreen.renderEntityInInventoryFollowsMouse(g, x1, y1, x2, y2,
                    Math.max(30, (y2 - y1) / 3), 0.0625f, mouseX, mouseY, minecraft.player);
            } finally {
                EmoteClient.endPreview();
            }
            String label = previewing == null ? "Hover an emote" : trim(previewing.name(), previewW - 12);
            g.drawString(font, label, previewX + previewW / 2 - font.width(label) / 2, y2 - 12, TEXT);
        } else {
            EmoteClient.setPreview(previewing);
        }

        int infoY = 60 + ((SLOTS + 1) / 2) * 22 + 8;
        if (selSlot >= 0) {
            g.drawString(font, "Filling slot " + (selSlot + 1), rx + 8, infoY, ACCENT);
            g.drawString(font, "Click emotes to assign", rx + 8, infoY + 12, TEXT_DIM);
        } else {
            g.drawString(font, "Pick a slot to edit", rx + 8, infoY, TEXT_DIM);
        }
        if (selected != null) {
            g.drawString(font, trim(selected.name(), panelRightW - 16), rx + 8, infoY + 28, TEXT);
            g.drawString(font, selected.category().label, rx + 8, infoY + 40, TEXT_DIM);
        } else {
            g.drawString(font, "Click an emote to select", rx + 8, infoY + 28, TEXT_DIM);
        }
    }

    @Override
    public void onClose() {
        persistMenuPage();
        EmoteConfig.get().save();
        EmoteClient.setPreview(null);
        minecraft.setScreen(parent);
    }
}
