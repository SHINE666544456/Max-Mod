package dev.skinoverride.mixin;

import dev.skinoverride.Freecam;
import dev.skinoverride.Zoom;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {

    /** While zooming or flying in freecam, the scroll wheel changes the zoom / speed instead of the hotbar slot. */
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void skinoverride$zoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (Freecam.onScroll(vertical) || Zoom.onScroll(vertical)) {
            ci.cancel();
        }
    }
}
