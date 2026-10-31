package dev.skinoverride.mixin;

import dev.skinoverride.ModConfig;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {

    /** "Low fire": the burning overlay is moved down so it only covers the bottom of the screen. */
    @Inject(method = "renderFireOverlay", at = @At("HEAD"))
    private static void skinoverride$lowFire(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                             Sprite sprite, CallbackInfo ci) {
        ModConfig cfg = ModConfig.get();
        if (cfg.lowFire) {
            matrices.translate(0.0F, -cfg.lowFireOffset / 100.0F, 0.0F);
        }
    }
}
