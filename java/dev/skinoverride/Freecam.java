package dev.skinoverride;

import dev.skinoverride.mixin.CameraAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/**
 * Detaches the camera from your player. Your character stays where it is (and is drawn), the camera
 * flies freely through everything. WASD moves, jump goes up, sneak goes down, sprint is a speed boost,
 * the scroll wheel changes the speed.
 */
public final class Freecam {

    private static boolean active;
    private static Vec3d pos = Vec3d.ZERO;
    private static Vec3d prevPos = Vec3d.ZERO;
    private static float yaw;
    private static float pitch;

    private Freecam() {
    }

    public static boolean active() {
        return active;
    }

    public static void toggle(MinecraftClient mc) {
        if (active) {
            disable(mc, "Freecam off");
        } else {
            enable(mc);
        }
    }

    private static void enable(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        pos = mc.player.getEyePos();
        prevPos = pos;
        yaw = mc.player.getYaw();
        pitch = mc.player.getPitch();
        active = true;
        message(mc, "Freecam on  -  speed " + ModConfig.get().freecamSpeed + " blocks/s (scroll to change)");
    }

    public static void disable(MinecraftClient mc, String reason) {
        if (!active) {
            return;
        }
        active = false;
        if (reason != null) {
            message(mc, reason);
        }
    }

    private static void message(MinecraftClient mc, String text) {
        if (mc.inGameHud != null) {
            mc.inGameHud.setOverlayMessage(Text.literal(text), false);
        }
    }

    /** Runs at the very start of every client tick, before the game reads the keys. */
    public static void startTick(MinecraftClient mc) {
        if (!active) {
            return;
        }
        ModConfig cfg = ModConfig.get();
        if (mc.player == null || mc.world == null) {
            active = false;
            return;
        }
        if (cfg.freecamDamageOff && mc.player.hurtTime > 0) {
            disable(mc, "Freecam off (you took damage)");
            return;
        }

        GameOptions o = mc.options;
        boolean fwd = false;
        boolean back = false;
        boolean left = false;
        boolean right = false;
        boolean up = false;
        boolean down = false;
        boolean boost = false;
        if (mc.currentScreen == null) {
            fwd = PhysicalKeys.isDown(mc, o.forwardKey);
            back = PhysicalKeys.isDown(mc, o.backKey);
            left = PhysicalKeys.isDown(mc, o.leftKey);
            right = PhysicalKeys.isDown(mc, o.rightKey);
            up = PhysicalKeys.isDown(mc, o.jumpKey);
            down = PhysicalKeys.isDown(mc, o.sneakKey);
            boost = PhysicalKeys.isDown(mc, o.sprintKey);
        }

        // The player must not move or act while you fly the camera around.
        release(o.forwardKey);
        release(o.backKey);
        release(o.leftKey);
        release(o.rightKey);
        release(o.jumpKey);
        release(o.sneakKey);
        release(o.sprintKey);
        if (cfg.freecamBlockActions) {
            release(o.attackKey);
            release(o.useKey);
            release(o.pickItemKey);
        }

        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double sy = Math.sin(yawRad);
        double cy = Math.cos(yawRad);
        double sp = Math.sin(pitchRad);
        double cp = Math.cos(pitchRad);
        Vec3d look = new Vec3d(-sy * cp, -sp, cy * cp);
        Vec3d side = new Vec3d(-cy, 0, -sy);

        double forward = (fwd ? 1 : 0) - (back ? 1 : 0);
        double strafe = (right ? 1 : 0) - (left ? 1 : 0);
        double vertical = (up ? 1 : 0) - (down ? 1 : 0);
        Vec3d move = look.multiply(forward).add(side.multiply(strafe)).add(0, vertical, 0);
        double length = move.length();
        prevPos = pos;
        if (length > 1.0e-4) {
            double perTick = cfg.freecamSpeed / 20.0 * (boost ? 3.0 : 1.0);
            pos = pos.add(move.multiply(perTick / Math.max(1.0, length)));
        }
    }

    private static void release(KeyBinding key) {
        key.setPressed(false);
        while (key.wasPressed()) {
            // swallow queued presses
        }
    }

    /** Mouse movement turns the camera instead of your player. */
    public static void look(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = Math.max(-90f, Math.min(90f, pitch + (float) dy * 0.15f));
    }

    /** Called from the camera hook once per frame. */
    public static void applyCamera(CameraAccessor camera, float tickDelta) {
        double t = Math.max(0f, Math.min(1f, tickDelta));
        Vec3d p = new Vec3d(
                prevPos.x + (pos.x - prevPos.x) * t,
                prevPos.y + (pos.y - prevPos.y) * t,
                prevPos.z + (pos.z - prevPos.z) * t);
        camera.skinoverride$setPos(p);
        camera.skinoverride$setRotation(yaw, pitch);
    }

    /** Scroll wheel changes the speed while flying. Returns true if the scroll was used. */
    public static boolean onScroll(double vertical) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!active || vertical == 0 || mc.currentScreen != null) {
            return false;
        }
        ModConfig cfg = ModConfig.get();
        int speed = cfg.freecamSpeed;
        int next = vertical > 0
                ? Math.max(speed + 1, Math.round(speed * 1.15f))
                : Math.min(speed - 1, Math.round(speed / 1.15f));
        cfg.freecamSpeed = Math.max(1, Math.min(100, next));
        cfg.save();
        message(mc, "Freecam speed: " + cfg.freecamSpeed + " blocks/s");
        return true;
    }
}
