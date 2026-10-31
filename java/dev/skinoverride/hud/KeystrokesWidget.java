package dev.skinoverride.hud;

import dev.skinoverride.ModConfig;
import dev.skinoverride.PhysicalKeys;
import dev.skinoverride.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

/** WASD, space and the mouse buttons as keys that light up when held, with clicks per second. */
public class KeystrokesWidget implements HudWidget {

    private static final int KEY = 20;
    private static final int GAP = 2;

    private static final Deque<Long> leftClicks = new ArrayDeque<>();
    private static final Deque<Long> rightClicks = new ArrayDeque<>();
    private static boolean leftWas;
    private static boolean rightWas;

    private boolean fwd;
    private boolean back;
    private boolean left;
    private boolean right;
    private boolean space;
    private boolean lmb;
    private boolean rmb;
    private int cpsLeft;
    private int cpsRight;
    private String kF = "W";
    private String kB = "S";
    private String kL = "A";
    private String kR = "D";
    private int w;
    private int h;

    @Override
    public String id() {
        return "keystrokes";
    }

    @Override
    public String label() {
        return "Keystrokes & CPS";
    }

    @Override
    public float defaultX() {
        return 0.02f;
    }

    @Override
    public float defaultY() {
        return 0.55f;
    }

    @Override
    public boolean enabled() {
        return ModConfig.get().keystrokesHud;
    }

    private static String shortLabel(KeyBinding kb) {
        String s = kb.getBoundKeyLocalizedText().getString().toUpperCase(Locale.ROOT);
        return s.length() <= 3 ? s : s.substring(0, 3);
    }

    private static int cps(Deque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000L) {
            clicks.pollFirst();
        }
        return clicks.size();
    }

    @Override
    public void prepare(MinecraftClient mc, boolean preview) {
        ModConfig cfg = ModConfig.get();
        GameOptions o = mc.options;
        long now = System.currentTimeMillis();
        kF = shortLabel(o.forwardKey);
        kB = shortLabel(o.backKey);
        kL = shortLabel(o.leftKey);
        kR = shortLabel(o.rightKey);

        if (preview || mc.player == null) {
            fwd = true;
            back = false;
            left = false;
            right = false;
            space = false;
            lmb = true;
            rmb = false;
            cpsLeft = 7;
            cpsRight = 2;
        } else {
            boolean inGame = mc.currentScreen == null;
            fwd = inGame && PhysicalKeys.isDown(mc, o.forwardKey);
            back = inGame && PhysicalKeys.isDown(mc, o.backKey);
            left = inGame && PhysicalKeys.isDown(mc, o.leftKey);
            right = inGame && PhysicalKeys.isDown(mc, o.rightKey);
            space = inGame && PhysicalKeys.isDown(mc, o.jumpKey);
            lmb = inGame && PhysicalKeys.isDown(mc, o.attackKey);
            rmb = inGame && PhysicalKeys.isDown(mc, o.useKey);
            // count button presses by watching for released -> pressed between frames
            if (lmb && !leftWas) {
                leftClicks.addLast(now);
            }
            if (rmb && !rightWas) {
                rightClicks.addLast(now);
            }
            leftWas = lmb;
            rightWas = rmb;
            cpsLeft = cps(leftClicks, now);
            cpsRight = cps(rightClicks, now);
        }

        w = KEY * 3 + GAP * 2;
        h = KEY * 2 + GAP;
        if (cfg.keystrokesSpace) {
            h += GAP + 14;
        }
        if (cfg.keystrokesMouse) {
            h += GAP + 22;
        }
    }

    @Override
    public int width() {
        return w;
    }

    @Override
    public int height() {
        return h;
    }

    private void key(DrawContext c, MinecraftClient mc, int x, int y, int bw, int bh, String label, boolean down) {
        Ui.roundRect(c, x, y, bw, bh, down ? Ui.ACCENT_FILL : Ui.BG);
        Ui.roundBorder(c, x, y, bw, bh, down ? Ui.ACCENT : Ui.BORDER);
        c.drawTextWithShadow(mc.textRenderer, label,
                x + (bw - mc.textRenderer.getWidth(label)) / 2, y + (bh - 8) / 2, down ? 0xFFFFFFFF : Ui.TEXT);
    }

    @Override
    public void render(DrawContext c, MinecraftClient mc, int x, int y, boolean preview) {
        ModConfig cfg = ModConfig.get();
        int top = y;
        key(c, mc, x + KEY + GAP, top, KEY, KEY, kF, fwd);
        top += KEY + GAP;
        key(c, mc, x, top, KEY, KEY, kL, left);
        key(c, mc, x + KEY + GAP, top, KEY, KEY, kB, back);
        key(c, mc, x + (KEY + GAP) * 2, top, KEY, KEY, kR, right);
        top += KEY;
        if (cfg.keystrokesSpace) {
            top += GAP;
            Ui.roundRect(c, x, top, w, 14, space ? Ui.ACCENT_FILL : Ui.BG);
            Ui.roundBorder(c, x, top, w, 14, space ? Ui.ACCENT : Ui.BORDER);
            c.fill(x + w / 2 - 12, top + 7, x + w / 2 + 12, top + 8, space ? 0xFFFFFFFF : Ui.MUTED);
            top += 14;
        }
        if (cfg.keystrokesMouse) {
            top += GAP;
            int half = (w - GAP) / 2;
            mouseKey(c, mc, x, top, half, "LMB", lmb, cpsLeft);
            mouseKey(c, mc, x + half + GAP, top, w - half - GAP, "RMB", rmb, cpsRight);
        }
    }

    private void mouseKey(DrawContext c, MinecraftClient mc, int x, int y, int bw, String label, boolean down, int cps) {
        Ui.roundRect(c, x, y, bw, 22, down ? Ui.ACCENT_FILL : Ui.BG);
        Ui.roundBorder(c, x, y, bw, 22, down ? Ui.ACCENT : Ui.BORDER);
        int color = down ? 0xFFFFFFFF : Ui.TEXT;
        if (ModConfig.get().keystrokesCps) {
            c.drawTextWithShadow(mc.textRenderer, label, x + (bw - mc.textRenderer.getWidth(label)) / 2, y + 3, color);
            String text = cps + " CPS";
            c.drawTextWithShadow(mc.textRenderer, text, x + (bw - mc.textRenderer.getWidth(text)) / 2, y + 12,
                    down ? 0xFFFFFFFF : Ui.MUTED);
        } else {
            c.drawTextWithShadow(mc.textRenderer, label, x + (bw - mc.textRenderer.getWidth(label)) / 2, y + 7, color);
        }
    }
}
