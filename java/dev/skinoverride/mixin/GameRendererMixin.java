package dev.skinoverride.mixin;

import dev.skinoverride.Zoom;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void skinoverride$zoom(Camera camera, float tickProgress, boolean changingFov,
                                   CallbackInfoReturnable<Float> cir) {
        float divisor = Zoom.divisor(tickProgress);
        if (divisor > 1.001f) {
            cir.setReturnValue(cir.getReturnValue() / divisor);
        }
    }
}
