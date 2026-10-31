package dev.skinoverride.hud;

import dev.skinoverride.ModConfig;
import dev.skinoverride.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Entity radar: a small scope that shows nearby players, hostile mobs, animals, villagers, dropped items and
 * other mobs, each category switchable. A tiny arrow marks things well above or below you.
 */
public class RadarWidget implements HudWidget {

    private static final int COLOR_PLAYER = 0xFFFFFFFF;
    private static final int COLOR_HOSTILE = 0xFFFF4040;
    private static final int COLOR_VILLAGER = 0xFFFFC857;
    private static final int COLOR_ANIMAL = 0xFF55DD66;
    private static final int COLOR_ITEM = 0xFF4DA3FF;
    private static final int COLOR_OTHER = 0xFFB69CFF;
    private static final int MAX_DRAWN = 150;

    private int size;

    @Override
    public String id() {
        return "radar";
    }

    @Override
    public String label() {
        return "Entity radar";
    }

    @Override
    public float defaultX() {
        return 1f;
    }

    @Override
    public float defaultY() {
        return 0.30f;
    }

    @Override
    public boolean enabled() {
        return ModConfig.get().radarOn;
    }

    @Override
    public void prepare(MinecraftClient mc, boolean preview) {
        size = Math.max(60, ModConfig.get().radarSize);
    }

    @Override
    public int width() {
        return size;
    }

    @Override
    public int height() {
        return size;
    }

    /** 0 = hidden by the filters, otherwise the dot color. */
    private static int colorOf(Entity e, ModConfig cfg) {
        if (e instanceof PlayerEntity) {
            return cfg.radarPlayers ? COLOR_PLAYER : 0;
        }
        if (e instanceof Monster) {
            return cfg.radarHostile ? COLOR_HOSTILE : 0;
        }
        if (e instanceof VillagerEntity || e instanceof WanderingTraderEntity) {
            return cfg.radarVillagers ? COLOR_VILLAGER : 0;
        }
        if (e instanceof AnimalEntity) {
            return cfg.radarAnimals ? COLOR_ANIMAL : 0;
        }
        if (e instanceof ItemEntity) {
            return cfg.radarItems ? COLOR_ITEM : 0;
        }
        if (e instanceof MobEntity) {
            return cfg.radarOther ? COLOR_OTHER : 0;
        }
        return 0;
    }

    @Override
    public void render(DrawContext c, MinecraftClient mc, int x, int y, boolean preview) {
        ModConfig cfg = ModConfig.get();
        int half = size / 2;
        int cx = x + half;
        int cy = y + half;

        Ui.roundRect(c, x - 1, y - 1, size + 2, size + 2, Ui.BG);
        c.fill(x, y, x + size, y + size, 0xB0080A10);
        // range rings and cross lines
        Ui.roundBorder(c, cx - half / 2, cy - half / 2, half, half, 0x40FFFFFF);
        c.fill(x, cy, x + size, cy + 1, 0x30FFFFFF);
        c.fill(cx, y, cx + 1, y + size, 0x30FFFFFF);

        if (preview || mc.player == null || mc.world == null) {
            dot(c, cx - half / 2, cy - half / 3, COLOR_HOSTILE);
            dot(c, cx + half / 3, cy + half / 4, COLOR_PLAYER);
            dot(c, cx + half / 2, cy - half / 2, COLOR_ANIMAL);
        } else {
            drawLive(c, mc, cfg, cx, cy, half);
        }

        // you
        c.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFF000000);
        c.fill(cx - 1, cy - 1, cx + 2, cy + 2, Ui.ACCENT);

        Ui.roundBorder(c, x - 1, y - 1, size + 2, size + 2, Ui.BORDER);
        c.fill(x - 1, y - 1, x + 11, y, Ui.ACCENT);
        c.fill(x - 1, y - 1, x, y + 11, Ui.ACCENT);
        String range = cfg.radarRange + "m";
        c.drawTextWithShadow(mc.textRenderer, range, x + size - 3 - mc.textRenderer.getWidth(range), y + size - 10, Ui.MUTED);
    }

    private void drawLive(DrawContext c, MinecraftClient mc, ModConfig cfg, int cx, int cy, int half) {
        float range = Math.max(8, cfg.radarRange);
        float ppb = (half - 3) / range;
        double yaw = Math.toRadians(mc.player.getYaw());
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double px0 = mc.player.getX();
        double py0 = mc.player.getY();
        double pz0 = mc.player.getZ();
        int drawn = 0;
        boolean names = cfg.radarNames && size >= 80;

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || drawn >= MAX_DRAWN) {
                continue;
            }
            int color = colorOf(e, cfg);
            if (color == 0) {
                continue;
            }
            double dx = e.getX() - px0;
            double dz = e.getZ() - pz0;
            double dy = e.getY() - py0;
            if (dx * dx + dz * dz > (double) range * range || Math.abs(dy) > cfg.radarHeight) {
                continue;
            }
            double sx;
            double sy;
            if (cfg.radarRotate) {
                sx = -dx * cos - dz * sin;
                sy = dx * sin - dz * cos;
            } else {
                sx = dx;
                sy = dz;
            }
            int ex = cx + (int) Math.round(sx * ppb);
            int ey = cy + (int) Math.round(sy * ppb);
            dot(c, ex, ey, color);
            if (dy > 4) {
                c.fill(ex, ey - 5, ex + 1, ey - 4, color);
                c.fill(ex - 1, ey - 4, ex + 2, ey - 3, color);
            } else if (dy < -4) {
                c.fill(ex - 1, ey + 3, ex + 2, ey + 4, color);
                c.fill(ex, ey + 4, ex + 1, ey + 5, color);
            }
            if (names && e instanceof PlayerEntity p) {
                String name = p.getName().getString();
                if (name.length() > 10) {
                    name = name.substring(0, 10);
                }
                c.drawTextWithShadow(mc.textRenderer, name, ex - mc.textRenderer.getWidth(name) / 2, ey + 5, 0xFFFFFFFF);
            }
            drawn++;
        }
        // north marker when the radar is not rotating
        if (!cfg.radarRotate) {
            c.drawTextWithShadow(mc.textRenderer, "N", cx - 3, cy - half + 2, Ui.ACCENT);
        }
    }

    private static void dot(DrawContext c, int x, int y, int color) {
        c.fill(x - 2, y - 2, x + 3, y + 3, 0xFF000000);
        c.fill(x - 1, y - 1, x + 2, y + 2, color);
    }
}
