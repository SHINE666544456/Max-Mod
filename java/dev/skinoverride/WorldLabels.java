package dev.skinoverride;

import dev.skinoverride.mixin.CameraAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * AFK tags and chat speech bubbles above other players' heads. They are drawn as 2D HUD labels
 * at the projected position of the player's head, so no world-render hooks are needed. Only players
 * you can see are labelled. Hidden together with the HUD (F1 and the clean screenshot).
 */
public final class WorldLabels {

    private record Bubble(String text, long untilMs) {
    }

    private static final Map<String, Bubble> bubbles = new HashMap<>();

    private WorldLabels() {
    }

    /** Called by the chat processor for each player chat line. */
    public static void push(String sender, String text) {
        String clean = text.strip();
        if (clean.isEmpty()) {
            return;
        }
        long until = System.currentTimeMillis() + ModConfig.get().chatBubbleSeconds * 1000L;
        bubbles.put(sender.toLowerCase(Locale.ROOT), new Bubble(clean, until));
    }

    private static Bubble bubbleFor(PlayerEntity p) {
        String name = p.getName().getString().toLowerCase(Locale.ROOT);
        Bubble b = bubbles.get(name);
        if (b != null) {
            return b;
        }
        // senders can carry team prefixes: "[Team] Name"
        for (Map.Entry<String, Bubble> e : bubbles.entrySet()) {
            for (String token : e.getKey().split("[^a-z0-9_]+")) {
                if (token.equals(name)) {
                    return e.getValue();
                }
            }
        }
        return null;
    }

    public static void render(DrawContext c, MinecraftClient mc) {
        ModConfig cfg = ModConfig.get();
        boolean tags = cfg.afkTracking && cfg.afkTags;
        boolean showBubbles = cfg.chatBubbles;
        long now = System.currentTimeMillis();
        bubbles.values().removeIf(b -> b.untilMs() < now);
        if ((!tags && !showBubbles) || mc.world == null || mc.player == null) {
            return;
        }
        Camera cam = mc.gameRenderer.getCamera();
        if (cam == null) {
            return;
        }
        Vec3d camPos = ((CameraAccessor) (Object) cam).skinoverride$getPos();
        if (camPos == null) {
            return;
        }

        float tp = mc.getRenderTickCounter().getTickProgress(false);
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        TextRenderer tr = mc.textRenderer;

        double yaw = Math.toRadians(cam.getYaw());
        double pitch = Math.toRadians(cam.getPitch());
        double sy = Math.sin(yaw);
        double cy = Math.cos(yaw);
        double sp = Math.sin(pitch);
        double cp = Math.cos(pitch);
        Vec3d f = new Vec3d(-sy * cp, -sp, cy * cp);
        Vec3d r = new Vec3d(-cy, 0, -sy);
        Vec3d u = new Vec3d(
                r.y * f.z - r.z * f.y,
                r.z * f.x - r.x * f.z,
                r.x * f.y - r.y * f.x);
        double fovRad = Math.toRadians(mc.options.getFov().getValue() / Zoom.divisor(tp));
        double tanHalf = Math.tan(fovRad / 2.0);
        double aspect = (double) sw / sh;

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) {
                continue;
            }
            double dist = Math.sqrt(mc.player.squaredDistanceTo(p));
            int idle = tags ? AfkTracker.idleSeconds(p.getUuid()) : -1;
            boolean afk = tags && AfkTracker.isAfk(p.getUuid()) && dist <= 48;
            Bubble bubble = showBubbles && dist <= cfg.chatBubbleRange ? bubbleFor(p) : null;
            if (!afk && bubble == null) {
                continue;
            }
            if (!mc.player.canSee(p)) {
                continue;
            }
            Vec3d head = p.getLerpedPos(tp).add(0, p.getHeight() + 0.9, 0);
            Vec3d d = head.subtract(camPos);
            double z = d.dotProduct(f);
            if (z < 0.3) {
                continue;
            }
            double ndcX = (d.dotProduct(r) / z) / (tanHalf * aspect);
            double ndcY = (d.dotProduct(u) / z) / tanHalf;
            if (Math.abs(ndcX) > 1.4 || Math.abs(ndcY) > 1.4) {
                continue;
            }
            int x = (int) Math.round((ndcX + 1.0) * 0.5 * sw);
            int y = (int) Math.round((1.0 - ndcY) * 0.5 * sh);

            if (afk) {
                drawTag(c, tr, "AFK " + Ui.formatShort(idle), x, y);
                y -= 14;
            }
            if (bubble != null) {
                drawBubble(c, tr, bubble.text(), x, y);
            }
        }
    }

    private static void drawTag(DrawContext c, TextRenderer tr, String text, int x, int y) {
        int w = tr.getWidth(text) + 8;
        int x0 = x - w / 2;
        Ui.roundRect(c, x0, y - 11, w, 11, 0xD0111420);
        Ui.roundBorder(c, x0, y - 11, w, 11, Ui.WARN);
        c.drawTextWithShadow(tr, text, x0 + 4, y - 9, Ui.WARN);
    }

    private static void drawBubble(DrawContext c, TextRenderer tr, String text, int x, int y) {
        List<String> lines = wrap(tr, text, 150, 4);
        int textW = 0;
        for (String l : lines) {
            textW = Math.max(textW, tr.getWidth(l));
        }
        int w = textW + 10;
        int h = lines.size() * 10 + 6;
        int x0 = x - w / 2;
        int y0 = y - h - 5;
        Ui.roundRect(c, x0, y0, w, h, 0xE6161B2B);
        Ui.roundBorder(c, x0, y0, w, h, Ui.ACCENT);
        // little tail pointing at the player
        c.fill(x - 3, y0 + h, x + 3, y0 + h + 1, Ui.ACCENT);
        c.fill(x - 2, y0 + h + 1, x + 2, y0 + h + 2, Ui.ACCENT);
        c.fill(x - 1, y0 + h + 2, x + 1, y0 + h + 3, Ui.ACCENT);
        int ty = y0 + 4;
        for (String l : lines) {
            c.drawTextWithShadow(tr, l, x0 + 5, ty, Ui.TEXT);
            ty += 10;
        }
    }

    private static List<String> wrap(TextRenderer tr, String text, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (tr.getWidth(candidate) <= maxWidth) {
                line = new StringBuilder(candidate);
                continue;
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
            }
            line = new StringBuilder(tr.getWidth(word) > maxWidth ? tr.trimToWidth(word, maxWidth) : word);
            if (lines.size() >= maxLines) {
                break;
            }
        }
        if (!line.isEmpty() && lines.size() < maxLines) {
            lines.add(line.toString());
        }
        if (lines.size() >= maxLines && text.length() > 0) {
            String last = lines.get(lines.size() - 1);
            if (!text.endsWith(last)) {
                lines.set(lines.size() - 1, tr.trimToWidth(last, maxWidth - tr.getWidth("...")) + "...");
            }
        }
        return lines;
    }
}
