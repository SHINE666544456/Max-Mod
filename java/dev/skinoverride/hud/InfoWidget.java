package dev.skinoverride.hud;

import dev.skinoverride.ModConfig;
import dev.skinoverride.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.world.LightType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Coordinates, facing direction, FPS and ping in one small movable panel. */
public class InfoWidget implements HudWidget {

    private static final String[] DIRECTIONS = {
            "South (+Z)", "West (-X)", "North (-Z)", "East (+X)"
    };

    private final List<String> lines = new ArrayList<>();
    private int w;
    private int h;

    @Override
    public String id() {
        return "info";
    }

    @Override
    public String label() {
        return "Coordinates & FPS";
    }

    @Override
    public float defaultX() {
        return 0f;
    }

    @Override
    public float defaultY() {
        return 0f;
    }

    @Override
    public boolean enabled() {
        return ModConfig.get().infoHud;
    }

    @Override
    public void prepare(MinecraftClient mc, boolean preview) {
        ModConfig cfg = ModConfig.get();
        lines.clear();
        boolean demo = preview || mc.player == null;

        if (cfg.infoCoords) {
            double x = demo ? 128.5 : mc.player.getX();
            double y = demo ? 64.0 : mc.player.getY();
            double z = demo ? -312.2 : mc.player.getZ();
            lines.add(String.format(Locale.ROOT, "XYZ  %.1f  %.1f  %.1f", x, y, z));
        }
        if (cfg.infoDirection) {
            float yaw = demo ? 180f : mc.player.getYaw();
            int index = (int) Math.floor(yaw * 4.0f / 360.0f + 0.5f) & 3;
            lines.add("Facing  " + DIRECTIONS[index]);
        }
        if (cfg.infoLight) {
            if (demo || mc.world == null) {
                lines.add("Light  7 (sky 15)");
            } else {
                var pos = mc.player.getBlockPos();
                lines.add("Light  " + mc.world.getLightLevel(LightType.BLOCK, pos)
                        + " (sky " + mc.world.getLightLevel(LightType.SKY, pos) + ")");
            }
        }
        if (cfg.infoFps) {
            lines.add((demo ? 144 : mc.getCurrentFps()) + " FPS");
        }
        if (cfg.infoPing) {
            int ping = 24;
            if (!demo) {
                ClientPlayNetworkHandler handler = mc.getNetworkHandler();
                PlayerListEntry entry = handler == null ? null : handler.getPlayerListEntry(mc.player.getUuid());
                ping = entry == null ? 0 : entry.getLatency();
            }
            lines.add(ping + " ms");
        }
        if (lines.isEmpty()) {
            w = 0;
            h = 0;
            return;
        }
        int max = 0;
        for (String l : lines) {
            max = Math.max(max, mc.textRenderer.getWidth(l));
        }
        w = max + 12;
        h = lines.size() * 11 + 7;
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
        Ui.panel(c, x, y, w, h);
        c.fill(x + 1, y + 1, x + 3, y + h - 1, Ui.ACCENT_DIM);
        int ty = y + 5;
        for (String l : lines) {
            c.drawTextWithShadow(mc.textRenderer, l, x + 8, ty, Ui.TEXT);
            ty += 11;
        }
    }
}
