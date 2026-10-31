package dev.skinoverride.mixin;

import dev.skinoverride.CleanScreenshot;
import dev.skinoverride.ModConfig;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    /**
     * The game hides name tags whenever the HUD is hidden. While a clean screenshot is taken, say "the HUD is on"
     * to that check, so player name tags stay visible (the HUD itself is hidden separately).
     */
    @Inject(method = "isHudEnabled", at = @At("RETURN"), cancellable = true, require = 0)
    private static void skinoverride$keepNameTags(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && CleanScreenshot.capturing() && ModConfig.get().shotKeepNames) {
            cir.setReturnValue(true);
        }
    }
}
