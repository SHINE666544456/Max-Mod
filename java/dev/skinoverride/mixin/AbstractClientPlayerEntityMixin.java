package dev.skinoverride.mixin;

import dev.skinoverride.Cosmetics;
import dev.skinoverride.SkinStore;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The renderer asks the entity for its skin every frame; we swap it for the assigned one. */
@Mixin(value = AbstractClientPlayerEntity.class, priority = 900)
public abstract class AbstractClientPlayerEntityMixin {

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void skinoverride$replaceSkin(CallbackInfoReturnable<SkinTextures> cir) {
        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;
        SkinTextures base = cir.getReturnValue();
        SkinTextures custom = SkinStore.override(self.getUuid(), base);
        SkinTextures cosmetic = Cosmetics.apply(self.getUuid(), custom != null ? custom : base);
        if (cosmetic != null) {
            cir.setReturnValue(cosmetic);
        } else if (custom != null) {
            cir.setReturnValue(custom);
        }
    }
}
