package dev.skinoverride.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {

    @Accessor("pos")
    Vec3d skinoverride$getPos();

    @Invoker("setPos")
    void skinoverride$setPos(Vec3d pos);

    @Invoker("setRotation")
    void skinoverride$setRotation(float yaw, float pitch);
}
