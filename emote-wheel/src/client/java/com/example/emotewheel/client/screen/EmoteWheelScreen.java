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
import com.example.emotewheel.client.IrisTheme;
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
    private boolean hubHover = false; // mouse is over the centre
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
        int hdx = mouseX - cx, hdy = mouseY - cy;
        hubHover = hdx * hdx + hdy * hdy < R_IN * R_IN;

        drawVignette(g, cx, cy);
        drawDisc(g, cx, cy);
        drawHub(g, cx, cy);
        drawSlices(g, cx, cy);
        drawTabs(g, cx, cy);
        // title to match menu branding
        String title = "IRIS Emote Wheel";
        g.drawString(font, title, cx - font.width(title) / 2, cy - R_HALO - 18, IrisTheme.accent());
        drawHint(g, cx, cy);
    }

    private void drawVignette(GuiGraphics g, int cx, int cy) {
        int s = R_HALO + 28;
        g.fill(cx - s, cy - s, cx + s, cy + s, 0x88000000);
    }

    // ---- the disc is the same shape every frame, so its strips are computed once and replayed ----
    private static int[] runDy, runX0, runX1, runSec;
    private static float[] runR;
    private static int runCount;

    // Warm slate / gold wedges — matches Emote Menu palette
    private static final int[][] HUES = {
        {38, 34, 52}, {48, 40, 56}, {32, 36, 48}, {56, 46, 40},
        {42, 38, 58}, {36, 42, 50}, {52, 44, 48}, {40, 36, 54}
    };

    // per-frame values, computed once in drawDisc instead of once per strip
    private final boolean[] frameFilled = new boolean[SLOTS];
    private float framePulse;

    private static void buildRuns() {
        if (runDy != null) return;
        int cap = 8192;
        int[] dys = new int[cap], x0s = new int[cap], x1s = new int[cap], secs = new int[cap];
        float[] rs = new float[cap];
        int n = 0;
        for (int dy = -R_HALO; dy <= R_HALO; dy += 2) { // 2px-tall bands: half the fill calls, same look
            int span2 = R_HALO * R_HALO - dy * dy;
            if (span2 < 0) continue;
            int half = (int) Math.sqrt(span2);
            int runStart = -half;
            int runSector = sectorAt(-half, dy);
            for (int dx = -half + 1; dx <= half + 1; dx++) {
                int sec = dx <= half ? sectorAt(dx, dy) : Integer.MIN_VALUE; // sentinel flushes the last run
                if (sec != runSector) {
                    if (runSector != -1 && dx > runStart) {
                        if (n == cap) { // grow
                            cap *= 2;
                            dys = java.util.Arrays.copyOf(dys, cap); x0s = java.util.Arrays.copyOf(x0s, cap);
                            x1s = java.util.Arrays.copyOf(x1s, cap); secs = java.util.Arrays.copyOf(secs, cap);
                            rs = java.util.Arrays.copyOf(rs, cap);
                        }
                        int mx = (runStart + dx - 1) / 2;
                        dys[n] = dy; x0s[n] = runStart; x1s[n] = dx; secs[n] = runSector;
                        rs[n] = (float) Math.sqrt((double) mx * mx + (double) dy * dy);
                        n++;
                    }
                    runStart = dx;
                    runSector = sec;
                }
            }
        }
        runDy = dys; runX0 = x0s; runX1 = x1s; runSec = secs; runR = rs; runCount = n;
    }

    private int frameAccent = 0x35D0BA;
    private int frameBg = 0x111420;

    private void drawDisc(GuiGraphics g, int cx, int cy) {
        IrisTheme.refresh();
        frameAccent = IrisTheme.accent() & 0xFFFFFF;
        frameBg = IrisTheme.bg() & 0xFFFFFF;
        buildRuns();
        for (int i = 0; i < SLOTS; i++) frameFilled[i] = slotEmote(i) != null;
        framePulse = hovered >= 0 ? 0.55f + 0.45f * (float) Math.sin(System.nanoTime() / 1.4e8) : 0f;
        for (int i = 0; i < runCount; i++) {
            int y = cy + runDy[i];
            g.fill(cx + runX0[i], y, cx + runX1[i], y + 2, colorFor(runSec[i], runR[i]));
        }
    }

    private int colorFor(int sector, double r) {
        if (sector == -3) {
            // soft accent halo, same as the menu panels
            float u = (float) ((r - R_OUT) / (double) (R_HALO - R_OUT));
            return IrisTheme.withAlpha(frameAccent, (int) (85 * (1f - u)));
        }
        if (sector == -2) {
            // hub: the panel background of the Iris theme
            return IrisTheme.withAlpha(frameBg, 0xF0);
        }
        boolean hover = sector == hovered;
        boolean filled = sector >= 0 && frameFilled[sector];
        float u = Mth.clamp((float) ((r - R_IN) / (double) (R_OUT - R_IN)), 0f, 1f);
        float pulse = hover ? framePulse : 0f;

        int rgb;
        int a;
        if (hover) {
            // accent pulse on hover
            a = 0xF0;
            rgb = IrisTheme.mix(IrisTheme.mix(frameBg, frameAccent, 0.62f), frameAccent, pulse);
        } else if (filled) {
            a = 0xD0;
            rgb = IrisTheme.mix(frameBg, frameAccent, 0.16f + 0.025f * Math.floorMod(sector, 4));
        } else {
            // empty slice: deep, slightly darker than the panel
            a = 0xAA;
            rgb = IrisTheme.mix(frameBg, 0x000000, 0.30f);
        }
        // brighter toward the rim, darker near the hub
        float boost = 0.75f + u * 0.50f;
        int cr = clamp255(((rgb >> 16) & 255) * boost);
        int cg = clamp255(((rgb >> 8) & 255) * boost);
        int cb = clamp255((rgb & 255) * boost);
        if (r < R_IN + 3) return argb(Math.min(255, a + 20), cr + 10, cg + 10, cb + 14);
        if (r > R_OUT - 3 && sector >= 0) return IrisTheme.withAlpha(frameAccent, Math.min(255, a + 30));
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
            drawCentered(g, trim(h.name(), 72), cx, cy + 2, IrisTheme.text(), true);
            drawCentered(g, h.category().label, cx, cy + 13, IrisTheme.muted(), false);
        } else if (playing()) {
            // QoL: while you're emoting the middle of the wheel is a Stop button
            drawCentered(g, "STOP", cx, cy - 8, hubHover ? 0xFFFF9090 : 0xFFE08080, true);
            drawCentered(g, "click / release", cx, cy + 4, IrisTheme.muted(), false);
        } else {
            drawCentered(g, "PAGE", cx, cy - 10, IrisTheme.muted(), false);
            drawCentered(g, String.valueOf(page + 1), cx, cy + 2, IrisTheme.accent(), true);
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
                int nameColor = on ? IrisTheme.accent() : IrisTheme.text();
                drawCentered(g, trim(e.name(), on ? 78 : 62), px, py + 2, nameColor, on);
                if (cfg.isFavorite(e.id())) {
                    drawCentered(g, "\u2605", px, py + 13, IrisTheme.accent(), false);
                }
            } else {
                drawCentered(g, "+", px, py - 6, on ? IrisTheme.accent() : IrisTheme.muted(), false);
                drawCentered(g, "empty", px, py + 4, IrisTheme.mutedA(0x99), false);
            }
            // slot index near the rim
            int nx = cx + (int) Math.round(Math.sin(ang) * (R_OUT - 11));
            int ny = cy - (int) Math.round(Math.cos(ang) * (R_OUT - 11));
            drawCentered(g, String.valueOf(i + 1), nx, ny - 4, on ? IrisTheme.accent() : IrisTheme.mutedA(0x99), false);
        }
    }

    private void drawTabs(GuiGraphics g, int cx, int cy) {
        int y = tabY(cy);
        int total = EmoteConfig.PAGES * (TAB_W + 6) - 6;
        int x0 = cx - total / 2;
        for (int p = 0; p < EmoteConfig.PAGES; p++) {
            int x = x0 + p * (TAB_W + 6);
            boolean on = p == page;
            g.fill(x, y, x + TAB_W, y + TAB_H, on ? IrisTheme.accentA(0xF0) : IrisTheme.bgA(0xD0));
            g.fill(x, y, x + TAB_W, y + 1, on ? 0xAAFFFFFF : 0x44FFFFFF);
            g.fill(x, y + TAB_H - 1, x + TAB_W, y + TAB_H, on ? IrisTheme.withAlpha(IrisTheme.mix(IrisTheme.accent(), 0x000000, 0.5f), 0xAA) : 0x22000000);
            // thin border
            int border = on ? IrisTheme.accent() : IrisTheme.border();
            g.fill(x, y, x + TAB_W, y + 1, border);
            g.fill(x, y + TAB_H - 1, x + TAB_W, y + TAB_H, border);
            g.fill(x, y, x + 1, y + TAB_H, border);
            g.fill(x + TAB_W - 1, y, x + TAB_W, y + TAB_H, border);
            drawCentered(g, String.valueOf(p + 1), x + TAB_W / 2, y + 4, on ? 0xFF101418 : IrisTheme.text(), false);
        }
    }

        private void drawHint(GuiGraphics g, int cx, int cy) {
        String s = "Release to play   •   Right-click edit   •   Scroll pages   •   E library";
        int bw = font.width(s) + 20;
        int y = cy + R_OUT + 16;
        int x0 = cx - bw / 2;
        // match menu panel chrome
        g.fill(x0, y - 4, x0 + bw, y + 13, IrisTheme.bgA(0xD0));
        g.fill(x0, y - 4, x0 + bw, y - 3, IrisTheme.accent());
        g.fill(x0, y + 12, x0 + bw, y + 13, IrisTheme.border());
        drawCentered(g, s, cx, y, IrisTheme.muted(), false);
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

    /** Is the local player in the middle of an emote right now? */
    private boolean playing() {
        return minecraft != null && minecraft.player != null && EmoteClient.current(minecraft.player.getUUID()) != null;
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
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && hubHover && playing()) {
            onClose();
            EmoteClient.stopLocal();
            return true;
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
            } else if (hubHover && playing()) {
                onClose();
                EmoteClient.stopLocal();   // flick to the middle and let go to cancel
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
