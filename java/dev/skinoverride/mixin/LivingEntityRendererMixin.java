package dev.skinoverride.mixin;

import dev.skinoverride.ModConfig;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.ArmorStandEntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-side only: draws invisible players and mobs as normal. Nothing changes for anyone else
 * and the invisibility effect itself still works for everybody who does not run this setting.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "isVisible", at = @At("HEAD"), cancellable = true)
    private void skinoverride$revealInvisible(LivingEntityRenderState state, CallbackInfoReturnable<Boolean> cir) {
        // Invisible armor stands are used as hidden decoration/markers; leave those alone.
        if (ModConfig.get().revealInvisible && !(state instanceof ArmorStandEntityRenderState)) {
            cir.setReturnValue(true);
        }
    }
}
