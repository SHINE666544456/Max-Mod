package dev.skinoverride.mixin;

import com.mojang.authlib.GameProfile;
import dev.skinoverride.Cosmetics;
import dev.skinoverride.SkinStore;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Same swap for the tab list and anywhere else the game reads the skin from the player list entry. */
@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {

    @Shadow
    public abstract GameProfile getProfile();

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void skinoverride$replaceSkin(CallbackInfoReturnable<SkinTextures> cir) {
        GameProfile profile = this.getProfile();
        if (profile == null) {
            return;
        }
        SkinTextures base = cir.getReturnValue();
        SkinTextures custom = SkinStore.override(profile.id(), base);
        SkinTextures cosmetic = Cosmetics.apply(profile.id(), custom != null ? custom : base);
        if (cosmetic != null) {
            cir.setReturnValue(cosmetic);
        } else if (custom != null) {
            cir.setReturnValue(custom);
        }
    }
}
