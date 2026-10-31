package dev.skinoverride.mixin;

import dev.skinoverride.Freecam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    /** While freecam is on, mouse movement turns the camera and leaves your player alone. */
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void skinoverride$freecamLook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (Freecam.active() && (Object) this == MinecraftClient.getInstance().player) {
            Freecam.look(cursorDeltaX, cursorDeltaY);
            ci.cancel();
        }
    }
}
