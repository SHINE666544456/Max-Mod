package com.example.emotewheel.client.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.example.emotewheel.client.MaidUtil;

import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Armor outfits, picked by item name (client side only):
 *  diamond "Hinata" = Hinata outfit, diamond "made"/"maid" = black maid, netherite "made"/"maid" = pink maid.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements DataComponentHolder {

    @Unique
    private static final ResourceKey<EquipmentAsset> HINATA_KEY =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath("emote_wheel", "hinata"));
    @Unique
    private static final ResourceKey<EquipmentAsset> MAID_PINK_KEY =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath("emote_wheel", "maid_pink"));
    @Unique
    private static final ResourceKey<EquipmentAsset> MAID_BLACK_KEY =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath("emote_wheel", "maid_black"));

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(DataComponentType<? extends T> type) {
        T value = this.getComponents().get(type);
        if (type != DataComponents.EQUIPPABLE || !(value instanceof Equippable eq)) return value;

        Optional<ResourceKey<EquipmentAsset>> id = eq.assetId();
        if (id.isEmpty()) return value;

        Component name = this.getComponents().get(DataComponents.CUSTOM_NAME);
        if (name == null) return value;

        ResourceKey<EquipmentAsset> replacement = null;
        if (id.get().equals(EquipmentAssets.DIAMOND)) {
            if ("Hinata".equals(name.getString())) replacement = HINATA_KEY;
            else if (MaidUtil.isMaidName(name))    replacement = MAID_BLACK_KEY;
        } else if (id.get().equals(EquipmentAssets.NETHERITE)) {
            if (MaidUtil.isMaidName(name))         replacement = MAID_PINK_KEY;
        }
        if (replacement == null) return value;

        return (T) new Equippable(
            eq.slot(), eq.equipSound(), Optional.of(replacement), eq.cameraOverlay(),
            eq.allowedEntities(), eq.dispensable(), eq.swappable(), eq.damageOnHurt(),
            eq.equipOnInteract(), eq.canBeSheared(), eq.shearingSound());
    }
}
