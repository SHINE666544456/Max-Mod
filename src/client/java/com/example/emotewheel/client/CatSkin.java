package com.example.emotewheel.client;

import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

/** The Sexy Jutsu skin: assets/emote_wheel/textures/skin/cat_girl.png (a classic-arms 64x64 skin). */
public final class CatSkin {
    private CatSkin() {}

    private static final ClientAsset.ResourceTexture TEXTURE =
        new ClientAsset.ResourceTexture(Identifier.fromNamespaceAndPath("emote_wheel", "skin/cat_girl"));

    private static PlayerSkin lastIn, lastOut;

    /** Same player, but wearing the cat-girl skin (keeps their cape/elytra). Cached so it isn't rebuilt every call. */
    public static PlayerSkin of(PlayerSkin original) {
        if (original == lastIn && lastOut != null) return lastOut;
        lastIn = original;
        lastOut = new PlayerSkin(TEXTURE, original.cape(), original.elytra(), PlayerModelType.WIDE, original.secure());
        return lastOut;
    }
}
