package dev.skinoverride;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Marks the floor around you with the block light level: red = 0 (hostile mobs can spawn),
 * yellow = 1 to 7 (dim), green = 8 and up. Drawn as small squares projected onto the screen.
 */
public final class LightOverlay {

    private record Mark(double x, double y, double z, int level) {
    }

    private static final List<Mark> MARKS = new ArrayList<>();
    private static int timer;

    private LightOverlay() {
    }

    public static void tick(MinecraftClient mc) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.lightOverlay || mc.world == null || mc.player == null) {
            MARKS.clear();
            return;
        }
        if (++timer < 6) {
            return;
        }
        timer = 0;

        World world = mc.world;
        BlockPos origin = mc.player.getBlockPos();
        int r = cfg.lightRadius;
        List<Mark> found = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r + 1) {
                    continue;
                }
                for (int dy = 2; dy >= -3; dy--) {
                    BlockPos pos = origin.add(dx, dy, dz);
                    BlockState state = world.getBlockState(pos);
                    if (!state.isAir()) {
                        continue;
                    }
                    BlockPos below = pos.down();
                    BlockState floor = world.getBlockState(below);
                    if (floor.isSideSolidFullSquare(world, below, Direction.UP)) {
                        found.add(new Mark(pos.getX() + 0.5, pos.getY() + 0.03, pos.getZ() + 0.5,
                                world.getLightLevel(LightType.BLOCK, pos)));
                        break;
                    }
                }
            }
        }
        MARKS.clear();
        MARKS.addAll(found);
    }

    public static void render(DrawContext c, MinecraftClient mc) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.lightOverlay || MARKS.isEmpty()) {
            return;
        }
        Projection proj = Projection.create(mc, mc.getRenderTickCounter().getTickProgress(false));
        if (proj == null) {
            return;
        }
        for (Mark m : MARKS) {
            double[] s = proj.project(new Vec3d(m.x(), m.y(), m.z()));
            if (s == null) {
                continue;
            }
            double blockPx = proj.focal / s[2];
            int width = (int) Math.max(2, Math.min(40, Math.round(blockPx * 0.6)));
            int height = Math.max(2, width / 2);
            int color = m.level() == 0 ? 0xFFFF3B3B : m.level() < 8 ? 0xFFFFD23B : 0xFF3BE070;
            int x = (int) Math.round(s[0]) - width / 2;
            int y = (int) Math.round(s[1]) - height / 2;
            c.fill(x, y, x + width, y + height, Ui.withAlpha(color, 0xB0));
            if (cfg.lightNumbers && width >= 14) {
                String text = String.valueOf(m.level());
                c.drawTextWithShadow(mc.textRenderer, text,
                        (int) Math.round(s[0]) - mc.textRenderer.getWidth(text) / 2,
                        (int) Math.round(s[1]) - 4, 0xFFFFFFFF);
            }
        }
    }
}
