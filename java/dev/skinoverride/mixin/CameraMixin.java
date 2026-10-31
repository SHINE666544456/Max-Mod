package dev.skinoverride.mixin;

import dev.skinoverride.Freecam;
import dev.skinoverride.ModConfig;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {

    /** After the game positions the camera, freecam moves it somewhere else. */
    @Inject(method = "update", at = @At("TAIL"))
    private void skinoverride$freecam(World area, Entity focusedEntity, boolean thirdPerson,
                                      boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (Freecam.active()) {
            Freecam.applyCamera((CameraAccessor) (Object) this, tickDelta);
        }
    }

    /** Makes the game draw your own character while the camera is away from it. */
    @Inject(method = "isThirdPerson", at = @At("HEAD"), cancellable = true)
    private void skinoverride$showBody(CallbackInfoReturnable<Boolean> cir) {
        if (Freecam.active() && ModConfig.get().freecamShowBody) {
            cir.setReturnValue(true);
        }
    }
}
