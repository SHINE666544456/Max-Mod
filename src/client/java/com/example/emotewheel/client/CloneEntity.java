package com.example.emotewheel.client;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;

/**
 * A shadow clone: a client-side-only fake player that copies its owner's position, pose, swings, armor and held items.
 * No physics collision; can be targeted/punched by real players (client-side poof). Never sent to the server.
 */
public class CloneEntity extends RemotePlayer {
    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final AbstractClientPlayer owner;
    private final double offX, offZ;
    private int age;

    public CloneEntity(ClientLevel level, AbstractClientPlayer owner, double offX, double offZ) {
        super(level, new GameProfile(UUID.randomUUID(), owner.getName().getString()));
        this.owner = owner;
        this.offX = offX;
        this.offZ = offZ;
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(false);
        double x = owner.getX() + offX, y = owner.getY(), z = owner.getZ() + offZ;
        this.setPos(x, y, z);
        this.xo = x; this.yo = y; this.zo = z;
        copyLook();
        copyEquipment();
    }

    public UUID ownerUuid() { return owner.getUUID(); }
    public AbstractClientPlayer owner() { return owner; }

    @Override
    public void tick() {
        // Position first, so the walking animation sees the owner's real per-tick movement.
        this.setPos(owner.getX() + offX, owner.getY(), owner.getZ() + offZ);
        super.tick();
        copyLook();
        if (++age % 5 == 0) copyEquipment();
    }

    private void copyLook() {
        this.setOnGround(owner.onGround());
        this.setYRot(owner.getYRot());
        this.setXRot(owner.getXRot());
        this.yRotO = owner.yRotO;
        this.xRotO = owner.xRotO;
        this.yBodyRot = owner.yBodyRot;
        this.yBodyRotO = owner.yBodyRotO;
        this.yHeadRot = owner.yHeadRot;
        this.yHeadRotO = owner.yHeadRotO;
        this.setPose(owner.getPose());
        this.setShiftKeyDown(owner.isShiftKeyDown());
        this.setSprinting(owner.isSprinting());
        this.swinging = owner.swinging;
        this.swingTime = owner.swingTime;
        this.attackAnim = owner.attackAnim;
    }

    private void copyEquipment() {
        for (EquipmentSlot slot : SLOTS) {
            ItemStack theirs = owner.getItemBySlot(slot);
            if (!ItemStack.matches(theirs, this.getItemBySlot(slot))) this.setItemSlot(slot, theirs.copy());
        }
    }

    // the clone wears whatever skin its owner currently has (including the cat-girl transformation)
    @Override
    public PlayerSkin getSkin() { return owner.getSkin(); }

    @Override public boolean isPickable() { return true; }  // so left-click can target the clone
    @Override public boolean isPushable() { return false; }
    @Override public boolean shouldShowName() { return false; }
    @Override public boolean isAttackable() { return true; }
    @Override public boolean skipAttackInteraction(net.minecraft.world.entity.Entity attacker) {
        // Handled client-side in CloneManager (poof). Block default interaction.
        return true;
    }
}
