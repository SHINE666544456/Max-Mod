package dev.skinoverride.hud;

import dev.skinoverride.ModConfig;
import dev.skinoverride.Ui;
import dev.skinoverride.map.MapData;
import dev.skinoverride.map.Waypoints;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.joml.Matrix3x2fStack;

import java.util.Locale;

/** A live minimap built from the chunks around you: rotates with you (or stays north-up), shows players, mobs and waypoints. */
public class MinimapWidget implements HudWidget {

    /** Blocks shown across the map at each zoom level 1..4. */
    private static final int[] REGION = {128, 96, 64, 40};

    private int w;
    private int h;

    @Override
    public String id() {
        return "minimap";
    }

    @Override
    public String label() {
        return "Minimap";
    }

    @Override
    public float defaultX() {
        return 0f;
    }

    @Override
    public float defaultY() {
        return 0.14f;
    }

    @Override
    public boolean enabled() {
        return ModConfig.get().mapOn;
    }

    public static int regionBlocks() {
        int zoom = Math.max(1, Math.min(4, ModConfig.get().mapZoom));
        return REGION[zoom - 1];
    }

    @Override
    public void prepare(MinecraftClient mc, boolean preview) {
        ModConfig cfg = ModConfig.get();
        w = cfg.mapSize;
        h = cfg.mapSize + (cfg.mapCoords ? 12 : 0);
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
        ModConfig cfg = ModConfig.get();
        int size = cfg.mapSize;
        Ui.roundRect(c, x - 1, y - 1, size + 2, size + 2, Ui.BG);

        if (preview || mc.player == null || !MapData.miniReady()) {
            c.fill(x, y, x + size, y + size, MapData.backgroundColor());
            String text = preview ? "MAP" : "scanning...";
            c.drawTextWithShadow(mc.textRenderer, text, x + (size - mc.textRenderer.getWidth(text)) / 2,
                    y + size / 2 - 4, Ui.MUTED);
        } else {
            drawLive(c, mc, x, y, size, cfg);
        }
        Ui.roundBorder(c, x - 1, y - 1, size + 2, size + 2, Ui.BORDER);
        c.fill(x - 1, y - 1, x + 11, y, Ui.ACCENT);
        c.fill(x - 1, y - 1, x, y + 11, Ui.ACCENT);
        if (!preview && MapData.cave()) {
            c.drawTextWithShadow(mc.textRenderer, "CAVE", x + size - 4 - mc.textRenderer.getWidth("CAVE"), y + 3, Ui.WARN);
        }

        if (cfg.mapCoords) {
            String coords = preview || mc.player == null ? "128, 64, -312"
                    : mc.player.getBlockX() + ", " + mc.player.getBlockY() + ", " + mc.player.getBlockZ();
            c.drawTextWithShadow(mc.textRenderer, coords, x + (size - mc.textRenderer.getWidth(coords)) / 2,
                    y + size + 3, Ui.TEXT);
        }
    }

    private void drawLive(DrawContext c, MinecraftClient mc, int x, int y, int size, ModConfig cfg) {
        int region = regionBlocks();
        boolean rotate = cfg.mapRotate;
        float phi = rotate ? (float) (-Math.PI - Math.toRadians(mc.player.getYaw())) : 0f;
        float k = rotate ? 1.42f : 1.0f;
        int quad = Math.round(size * k);
        int regionPx = Math.min(MapData.TEX, Math.round(region * k));
        float u = (MapData.TEX - regionPx) / 2f;

        Matrix3x2fStack m = c.getMatrices();
        c.enableScissor(x, y, x + size, y + size);
        m.pushMatrix();
        m.translate(x + size / 2f, y + size / 2f);
        m.rotate(phi);
        c.drawTexture(RenderPipelines.GUI_TEXTURED, MapData.miniId(), -quad / 2, -quad / 2, u, u,
                quad, quad, regionPx, regionPx, MapData.TEX, MapData.TEX);
        m.popMatrix();

        float cx = x + size / 2f;
        float cy = y + size / 2f;
        float ppb = size / (float) region;
        float cos = (float) Math.cos(phi);
        float sin = (float) Math.sin(phi);
        double baseX = MapData.miniCenterX() + 0.5;
        double baseZ = MapData.miniCenterZ() + 0.5;
        int half = size / 2;

        if (cfg.mapWaypoints) {
            for (Waypoints.Waypoint wp : Waypoints.list()) {
                float[] p = place(wp.x + 0.5 - baseX, wp.z + 0.5 - baseZ, ppb, cos, sin, cx, cy, half - 4, true);
                int px = Math.round(p[0]);
                int py = Math.round(p[1]);
                c.fill(px - 3, py - 3, px + 4, py + 4, 0xFF000000);
                c.fill(px - 2, py - 2, px + 3, py + 3, 0xFF000000 | wp.color);
            }
        }
        if (cfg.mapPlayers || cfg.mapMobs) {
            for (Entity e : mc.world.getEntities()) {
                if (e == mc.player) {
                    continue;
                }
                boolean isPlayer = e instanceof PlayerEntity;
                boolean mob = e instanceof MobEntity;
                if (!((isPlayer && cfg.mapPlayers) || (mob && cfg.mapMobs))) {
                    continue;
                }
                float[] p = place(e.getX() - baseX, e.getZ() - baseZ, ppb, cos, sin, cx, cy, half - 3, false);
                if (p == null) {
                    continue;
                }
                int color = isPlayer ? 0xFFFFFFFF : e instanceof HostileEntity ? 0xFFFF4040 : 0xFF55DD66;
                int px = Math.round(p[0]);
                int py = Math.round(p[1]);
                c.fill(px - 2, py - 2, px + 3, py + 3, 0xFF000000);
                c.fill(px - 1, py - 1, px + 2, py + 2, color);
            }
        }

        // you
        float[] me = place(mc.player.getX() - baseX, mc.player.getZ() - baseZ, ppb, cos, sin, cx, cy, half - 3, false);
        if (me != null) {
            double yaw = Math.toRadians(mc.player.getYaw());
            double fx = -Math.sin(yaw);
            double fz = Math.cos(yaw);
            double rx = fx * cos - fz * sin;
            double rz = fx * sin + fz * cos;
            float psi = (float) (Math.atan2(rz, rx) + Math.PI / 2);
            m.pushMatrix();
            m.translate(me[0], me[1]);
            m.rotate(psi);
            for (int i = -5; i <= 3; i++) {
                int wdt = (i + 5) / 2 + 1;
                c.fill(-wdt, i, wdt + 1, i + 1, 0xFF000000);
            }
            for (int i = -4; i <= 3; i++) {
                int wdt = (i + 4) / 2;
                c.fill(-wdt, i, wdt + 1, i + 1, 0xFFFFFFFF);
            }
            m.popMatrix();
        }

        // north marker
        float nx = cx + (float) (0 * cos - (-1) * sin) * (half - 7);
        float ny = cy + (float) (0 * sin + (-1) * cos) * (half - 7);
        c.drawTextWithShadow(mc.textRenderer, "N", Math.round(nx) - 3, Math.round(ny) - 4, Ui.ACCENT);
        c.disableScissor();
    }

    /** Screen position of a world offset; if clampToEdge the point is pushed to the map edge instead of dropped. */
    private static float[] place(double dx, double dz, float ppb, float cos, float sin,
                                 float cx, float cy, int limit, boolean clampToEdge) {
        float sx = (float) dx * ppb;
        float sy = (float) dz * ppb;
        float rx = sx * cos - sy * sin;
        float ry = sx * sin + sy * cos;
        float biggest = Math.max(Math.abs(rx), Math.abs(ry));
        if (biggest > limit) {
            if (!clampToEdge) {
                return null;
            }
            float f = limit / biggest;
            rx *= f;
            ry *= f;
        }
        return new float[]{cx + rx, cy + ry};
    }

    /** Used by the keybinds to show the zoom level. */
    public static String zoomText() {
        return String.format(Locale.ROOT, "Minimap zoom %d/4 (%d blocks)", ModConfig.get().mapZoom, regionBlocks());
    }
}
