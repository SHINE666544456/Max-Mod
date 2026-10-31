package dev.skinoverride;

import dev.skinoverride.mixin.CameraAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

/** Projects world positions onto the GUI so 2D overlays can be drawn at them (uses the real camera, so it follows freecam). */
public final class Projection {

    public final Vec3d cam;
    private final Vec3d f;
    private final Vec3d r;
    private final Vec3d u;
    private final double tanHalf;
    private final double aspect;
    private final int sw;
    private final int sh;
    /** GUI pixels covered by one block at depth 1. */
    public final double focal;

    private Projection(Vec3d cam, Vec3d f, Vec3d r, Vec3d u, double tanHalf, int sw, int sh) {
        this.cam = cam;
        this.f = f;
        this.r = r;
        this.u = u;
        this.tanHalf = tanHalf;
        this.sw = sw;
        this.sh = sh;
        this.aspect = (double) sw / sh;
        this.focal = (sh / 2.0) / tanHalf;
    }

    public static Projection create(MinecraftClient mc, float tickProgress) {
        Camera cam = mc.gameRenderer.getCamera();
        if (cam == null) {
            return null;
        }
        Vec3d pos = ((CameraAccessor) (Object) cam).skinoverride$getPos();
        if (pos == null) {
            return null;
        }
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
        double fovRad = Math.toRadians(mc.options.getFov().getValue() / Zoom.divisor(tickProgress));
        return new Projection(pos, f, r, u, Math.tan(fovRad / 2.0),
                mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
    }

    /** Returns {screenX, screenY, depth} or null when the point is behind or far outside the screen. */
    public double[] project(Vec3d world) {
        Vec3d d = world.subtract(cam);
        double z = d.dotProduct(f);
        if (z < 0.3) {
            return null;
        }
        double ndcX = (d.dotProduct(r) / z) / (tanHalf * aspect);
        double ndcY = (d.dotProduct(u) / z) / tanHalf;
        if (Math.abs(ndcX) > 1.4 || Math.abs(ndcY) > 1.4) {
            return null;
        }
        return new double[]{(ndcX + 1.0) * 0.5 * sw, (1.0 - ndcY) * 0.5 * sh, z};
    }
}
