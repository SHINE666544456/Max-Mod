package dev.skinoverride.map;

import dev.skinoverride.ModConfig;
import dev.skinoverride.Projection;
import dev.skinoverride.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

/** Waypoint names and distances floating where the waypoints are in the world. */
public final class WaypointLabels {

    private WaypointLabels() {
    }

    public static void render(DrawContext c, MinecraftClient mc) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.mapOn || !cfg.waypointLabels || mc.player == null) {
            return;
        }
        var list = Waypoints.list();
        if (list.isEmpty()) {
            return;
        }
        Projection proj = Projection.create(mc, mc.getRenderTickCounter().getTickProgress(false));
        if (proj == null) {
            return;
        }
        for (Waypoints.Waypoint wp : list) {
            double dx = wp.x + 0.5 - mc.player.getX();
            double dy = wp.y + 0.5 - mc.player.getY();
            double dz = wp.z + 0.5 - mc.player.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist < 2.5 || dist > 4000) {
                continue;
            }
            double[] s = proj.project(new Vec3d(wp.x + 0.5, wp.y + 1.6, wp.z + 0.5));
            if (s == null) {
                continue;
            }
            int x = (int) Math.round(s[0]);
            int y = (int) Math.round(s[1]);
            int color = 0xFF000000 | wp.color;
            c.fill(x - 3, y - 3, x + 4, y + 4, 0xFF000000);
            c.fill(x - 2, y - 2, x + 3, y + 3, color);
            String text = wp.name + "  " + Math.round(dist) + "m";
            int tw = mc.textRenderer.getWidth(text);
            Ui.roundRect(c, x - tw / 2 - 3, y + 6, tw + 6, 11, 0xA0000000);
            c.drawTextWithShadow(mc.textRenderer, text, x - tw / 2, y + 8, color);
        }
    }
}
