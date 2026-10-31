package dev.skinoverride.mixin;

import dev.skinoverride.ModConfig;
import dev.skinoverride.chat.ChatColors;
import dev.skinoverride.chat.ChatProcessor;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    /** Recolors other players' nametags (solid, rainbow or cycling). Only affects your own screen. */
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void skinoverride$nametagColor(Entity entity, CallbackInfoReturnable<Text> cir) {
        if (!(entity instanceof AbstractClientPlayerEntity player)) {
            return;
        }
        ModConfig cfg = ModConfig.get();
        ModConfig.PlayerChat pc = ChatProcessor.lookup(player.getName().getString(), cfg);
        int mode = pc != null && pc.tagColor != ChatColors.DEFAULT ? pc.tagColor : cfg.nametagDefaultColor;
        if (mode != ChatColors.DEFAULT && cir.getReturnValue() != null) {
            cir.setReturnValue(ChatColors.apply(cir.getReturnValue(), mode));
        }
    }
}
