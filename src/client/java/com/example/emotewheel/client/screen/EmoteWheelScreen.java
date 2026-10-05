package com.example.emotewheel.client.screen;

import org.lwjgl.glfw.GLFW;

import com.example.emotewheel.client.EmoteClient;
import com.example.emotewheel.client.EmoteAccess;
import com.example.emotewheel.client.EmoteConfig;
import com.example.emotewheel.client.EmoteKeys;
import com.example.emotewheel.client.Icons;
import com.example.emotewheel.emote.Emote;
import com.example.emotewheel.emote.Emotes;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The radial emote wheel. Hold the key, move the mouse toward a slice and let go to play it.
 * (Or tap the key and click a slice.) Right-click a slice to edit it; empty slices open the editor.
 */
public class EmoteWheelScreen extends Screen {
    private static final int SLOTS = EmoteConfig.SLOTS;
    private static final int R_OUT = 118, R_IN = 40, R_HALO = 132, LABEL_R = 80;
    private static final double TWO_PI = Math.PI * 2;
    private static final int TAB_W = 36, TAB_H = 16;

    private int page;
    private int hovered = -1;
    private boolean wheelKeyDown = true;
    private final long openedAt = System.nanoTime();

    public EmoteWheelScreen() {
        super(Component.literal("Emote Wheel"));
        this.page = EmoteConfig.get().page;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        // world stays visible
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int cx = width / 2, cy = height / 2;
        hovered = slotAt(mouseX - cx, mouseY - cy);

        drawVignette(g, cx, cy);
        drawDisc(g, cx, cy);
        drawHub(g, cx, cy);
        drawSlices(g, cx, cy);
        drawTabs(g, cx, cy);
        drawHint(g, cx, cy);
    }

    private void drawVignette(GuiGraphics g, int cx, int cy) {
        int s = R_HALO + 28;
        g.fill(cx - s, cy - s, cx + s, cy + s, 0x66000000);
    }

    private void drawDisc(GuiGraphics g, int cx, int cy) {
        for (int dy = -R_HALO; dy <= R_HALO; dy++) {
            int span2 = R_HALO * R_HALO - dy * dy;
            if (span2 < 0) continue;
            int half = (int) Math.sqrt(span2);
            int runStart = -half;
            int runSector = sectorAt(-half, dy);
            for (int dx = -half + 1; dx <= half; dx++) {
                int s = sectorAt(dx, dy);
                if (s != runSector) {
                    flush(g, cx, cy, runStart, dx, dy, runSector);
                    runStart = dx;
                    runSector = s;
                }
            }
            flush(g, cx, cy, runStart, half + 1, dy, runSector);
        }
    }

    private void flush(GuiGraphics g, int cx, int cy, int x0, int x1, int dy, int sector) {
        if (sector == -1 || x1 <= x0) return;
        int mx = (x0 + x1 - 1) / 2;
        double r = Math.sqrt((double) mx * mx + (double) dy * dy);
        int color = colorFor(sector, r);
        g.fill(cx + x0, cy + dy, cx + x1, cy + dy + 1, color);
    }

    private int colorFor(int sector, double r) {
        if (sector == -3) {
            float u = (float) ((r - R_OUT) / (double) (R_HALO - R_OUT));
            int a = (int) (70 * (1f - u));
            return argb(a, 232, 196, 120);
        }
        if (sector == -2) {
            return 0xF2141022;
        }
        boolean hover = sector == hovered;
        boolean filled = slotEmote(sector) != null;
        float u = Mth.clamp((float) ((r - R_IN) / (double) (R_OUT - R_IN)), 0f, 1f);
        float pulse = hover ? 0.55f + 0.45f * (float) Math.sin(System.nanoTime() / 1.4e8) : 0f;

        int cr, cg, cb, a;
        if (hover) {
            a = 0xF0;
            cr = (int) (70 + 90 * pulse);
            cg = (int) (90 + 70 * pulse);
            cb = 255;
        } else if (filled) {
            a = 0xCC;
            // slight hue shift per slice so the pie reads as separate wedges
            int[][] hues = {
                {42, 28, 72}, {28, 36, 78}, {24, 52, 64}, {56, 28, 48},
                {36, 24, 70}, {22, 44, 72}, {48, 32, 58}, {30, 30, 68}
            };
            int[] h = hues[Math.floorMod(sector, hues.length)];
            cr = h[0]; cg = h[1]; cb = h[2];
        } else {
            a = 0x99;
            cr = 16; cg = 14; cb = 24;
        }
        // brighter toward the rim, darker near the hub
        float boost = 0.72f + u * 0.55f;
        cr = clamp255(cr * boost);
        cg = clamp255(cg * boost);
        cb = clamp255(cb * boost);
        if (r < R_IN + 3) return argb(Math.min(255, a + 20), cr + 12, cg + 10, cb + 16);
        if (r > R_OUT - 3 && sector >= 0) return argb(Math.min(255, a + 30), 210, 180, 110);
        return argb(a, cr, cg, cb);
    }

    private static int clamp255(float v) { return Math.max(0, Math.min(255, (int) v)); }

    private static int argb(int a, int r, int g, int b) {
        return ((a & 255) << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
    }

    private void drawHub(GuiGraphics g, int cx, int cy) {
        Emote h = hovered >= 0 ? slotEmote(hovered) : null;
        int r = R_IN - 2;
        g.fill(cx - r, cy - r, cx + r, cy + r, 0x00000000); // disc already painted the hub

        if (h != null) {
            g.renderItem(Icons.of(h), cx - 8, cy - 18);
            drawCentered(g, trim(h.name(), 72), cx, cy + 2, 0xFFFFF4C8, true);
            drawCentered(g, h.category().label, cx, cy + 13, 0xFFB8C0E0, false);
        } else {
            drawCentered(g, "PAGE", cx, cy - 10, 0xFF8A90B0, false);
            drawCentered(g, String.valueOf(page + 1), cx, cy + 2, 0xFFFFE8A0, true);
        }
    }

    private void drawSlices(GuiGraphics g, int cx, int cy) {
        EmoteConfig cfg = EmoteConfig.get();
        for (int i = 0; i < SLOTS; i++) {
            double ang = i * TWO_PI / SLOTS;
            float pop = (i == hovered) ? 6f : 0f;
            int px = cx + (int) Math.round(Math.sin(ang) * (LABEL_R + pop));
            int py = cy - (int) Math.round(Math.cos(ang) * (LABEL_R + pop));
            Emote e = slotEmote(i);
            boolean on = i == hovered;
            if (e != null) {
                g.renderItem(Icons.of(e), px - 8, py - 18);
                int nameColor = on ? 0xFFFFF0C0 : 0xFFF4F6FF;
                drawCentered(g, trim(e.name(), on ? 78 : 62), px, py + 2, nameColor, on);
                if (cfg.isFavorite(e.id())) {
                    drawCentered(g, "\u2605", px, py + 13, 0xFFFFD060, false);
                }
            } else {
                drawCentered(g, "+", px, py - 6, on ? 0xFFFFE080 : 0xFF6A7088, false);
                drawCentered(g, "empty", px, py + 4, 0xFF5A6078, false);
            }
            // slot index near the rim
            int nx = cx + (int) Math.round(Math.sin(ang) * (R_OUT - 11));
            int ny = cy - (int) Math.round(Math.cos(ang) * (R_OUT - 11));
            drawCentered(g, String.valueOf(i + 1), nx, ny - 4, on ? 0xFFFFE8A0 : 0x88C8D0E8, false);
        }
    }

    private void drawTabs(GuiGraphics g, int cx, int cy) {
        int y = tabY(cy);
        int total = EmoteConfig.PAGES * (TAB_W + 6) - 6;
        int x0 = cx - total / 2;
        for (int p = 0; p < EmoteConfig.PAGES; p++) {
            int x = x0 + p * (TAB_W + 6);
            boolean on = p == page;
            g.fill(x, y, x + TAB_W, y + TAB_H, on ? 0xF0E8C070 : 0xC0181428);
            g.fill(x, y, x + TAB_W, y + 1, on ? 0xFFFFF0C0 : 0x44FFFFFF);
            g.fill(x, y + TAB_H - 1, x + TAB_W, y + TAB_H, on ? 0xAA806020 : 0x22000000);
            drawCentered(g, String.valueOf(p + 1), x + TAB_W / 2, y + 4, on ? 0xFF201810 : 0xFFE8ECF8, false);
        }
    }

    private void drawHint(GuiGraphics g, int cx, int cy) {
        String s = "Release to play   \u2022   Right-click edit   \u2022   Scroll pages   \u2022   E library";
        int w = font.width(s) + 16;
        int y = cy + R_OUT + 16;
        int x0 = cx - w / 2;
        g.fill(x0, y - 3, x0 + w, y + 12, 0xAA100C18);
        drawCentered(g, s, cx, y, 0xFFC8D0E8, false);
    }

    /** -3 halo, -2 hub, -1 gap, else slice index. */
    private static int sectorAt(int dx, int dy) {
        double r2 = (double) dx * dx + (double) dy * dy;
        if (r2 > (double) R_HALO * R_HALO) return -1;
        if (r2 > (double) R_OUT * R_OUT) return -3;
        if (r2 < (double) R_IN * R_IN) return -2;
        double r = Math.sqrt(r2);
        double w = TWO_PI / SLOTS;
        int idx = indexForAngle(dx, dy);
        double a = Math.atan2(dx, -dy);
        if (a < 0) a += TWO_PI;
        double a2 = (a + w / 2) % TWO_PI;
        double rem = a2 - idx * w;
        if (Math.min(rem, w - rem) * r < 1.6) return -1;
        return idx;
    }

    private static int indexForAngle(int dx, int dy) {
        double w = TWO_PI / SLOTS;
        double a = Math.atan2(dx, -dy);
        if (a < 0) a += TWO_PI;
        return (int) (((a + w / 2) % TWO_PI) / w);
    }

    private static int slotAt(int dx, int dy) {
        double r2 = (double) dx * dx + (double) dy * dy;
        if (r2 < (double) R_IN * R_IN || r2 > (double) R_OUT * R_OUT) return -1;
        return indexForAngle(dx, dy);
    }

    private Emote slotEmote(int slot) {
        Emote e = Emotes.byId(EmoteConfig.get().slotId(page, slot));
        return EmoteAccess.canUse(e) ? e : null;
    }

    private int tabX0(int cx) {
        int total = EmoteConfig.PAGES * (TAB_W + 6) - 6;
        return cx - total / 2;
    }

    private static int tabY(int cy) { return cy - R_OUT - 28; }

    private void drawCentered(GuiGraphics g, String s, int x, int y, int color, boolean shadow) {
        g.drawString(font, s, x - font.width(s) / 2, y, color, shadow);
    }

    private String trim(String s, int max) {
        if (font.width(s) <= max) return s;
        while (s.length() > 1 && font.width(s + "…") > max) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int cx = width / 2, cy = height / 2;
        int mx = (int) event.x(), my = (int) event.y();

        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int x0 = tabX0(cx), y = tabY(cy);
            for (int p = 0; p < EmoteConfig.PAGES; p++) {
                int x = x0 + p * (TAB_W + 6);
                if (mx >= x && mx < x + TAB_W && my >= y && my < y + TAB_H) { setPage(p); return true; }
            }
        }
        int slot = slotAt(mx - cx, my - cy);
        if (slot >= 0) {
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) { openEditor(slot); return true; }
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) { activate(slot); return true; }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (EmoteKeys.WHEEL.matches(event)) {
            if (!wheelKeyDown) onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_1) { setPage(0); return true; }
        if (key == GLFW.GLFW_KEY_2) { setPage(1); return true; }
        if (key == GLFW.GLFW_KEY_3) { setPage(2); return true; }
        if (key == GLFW.GLFW_KEY_LEFT) { setPage(page - 1); return true; }
        if (key == GLFW.GLFW_KEY_RIGHT) { setPage(page + 1); return true; }
        if (key == GLFW.GLFW_KEY_E || EmoteKeys.MENU.matches(event)) { openEditor(-1); return true; }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (EmoteKeys.WHEEL.matches(event)) {
            boolean wasHeld = wheelKeyDown;
            wheelKeyDown = false;
            if (!wasHeld) return true;
            if (hovered >= 0) {
                activate(hovered);
            } else if ((System.nanoTime() - openedAt) > 350_000_000L) {
                onClose();
            }
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            setPage(page + (scrollY > 0 ? -1 : 1));
            return true;
        }
        return false;
    }

    private void setPage(int p) {
        page = Math.floorMod(p, EmoteConfig.PAGES);
        EmoteConfig cfg = EmoteConfig.get();
        cfg.page = page;
        cfg.save();
    }

    private void activate(int slot) {
        Emote e = slotEmote(slot);
        if (e == null) {
            openEditor(slot);
            return;
        }
        onClose();
        EmoteClient.playLocal(e);
    }

    private void openEditor(int slot) {
        minecraft.setScreen(new EmoteMenuScreen(null, slot));
    }
}
