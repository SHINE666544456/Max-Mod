package com.example.emotewheel.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.example.emotewheel.States;
import com.example.emotewheel.client.CatSkin;
import com.example.emotewheel.client.ClientStates;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;

/** The Sexy Jutsu: while the cat_girl state is on, this player wears the Cat_Girl skin (for everyone with the mod). */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    @Inject(method = "getSkin()Lnet/minecraft/world/entity/player/PlayerSkin;", at = @At("RETURN"), cancellable = true)
    private void emotes$catGirlSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        if (ClientStates.has(self.getUUID(), States.CAT_GIRL)) {
            cir.setReturnValue(CatSkin.of(cir.getReturnValue()));
        }
    }
}
