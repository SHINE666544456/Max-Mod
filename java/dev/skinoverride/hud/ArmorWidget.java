package dev.skinoverride.hud;

import dev.skinoverride.ModConfig;
import dev.skinoverride.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Your worn armor plus held items with durability numbers and a flashing low-durability warning.
 * Can be a horizontal row or a vertical column, and be dragged anywhere (e.g. beside the hotbar).
 */
public class ArmorWidget implements HudWidget {

    private static final int SLOT = 22;
    private static final int GAP = 2;
    private static final int GROUP_GAP = 6;
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final List<ItemStack> stacks = new ArrayList<>();
    private int armorCount;
    private boolean vertical;
    private int labelW;
    private List<ItemStack> demo;
    private int w;
    private int h;

    @Override
    public String id() {
        return "armor";
    }

    @Override
    public String label() {
        return "Armor & held items";
    }

    @Override
    public float defaultX() {
        return 0f;
    }

    @Override
    public float defaultY() {
        return 1f;
    }

    @Override
    public boolean enabled() {
        return ModConfig.get().armorHud;
    }

    @Override
    public boolean visible(MinecraftClient mc) {
        // The inventory already shows your armor.
        return !(mc.currentScreen instanceof HandledScreen<?>);
    }

    private static String durabilityText(ItemStack stack, ModConfig cfg) {
        if (cfg.armorDurability == 0 || stack.getMaxDamage() <= 0) {
            return "";
        }
        int left = stack.getMaxDamage() - stack.getDamage();
        return cfg.armorDurability == 2 ? (left * 100 / stack.getMaxDamage()) + "%" : String.valueOf(left);
    }

    @Override
    public void prepare(MinecraftClient mc, boolean preview) {
        ModConfig cfg = ModConfig.get();
        stacks.clear();
        armorCount = 0;
        vertical = cfg.armorLayout == 1;

        if (preview || mc.player == null) {
            if (demo == null) {
                ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
                helmet.setDamage(helmet.getMaxDamage() * 92 / 100);
                demo = List.of(helmet, new ItemStack(Items.DIAMOND_CHESTPLATE),
                        new ItemStack(Items.DIAMOND_LEGGINGS), new ItemStack(Items.DIAMOND_BOOTS),
                        new ItemStack(Items.DIAMOND_SWORD));
            }
            stacks.addAll(demo.subList(0, 4));
            armorCount = 4;
            if (cfg.armorHeld) {
                stacks.add(demo.get(4));
            }
        } else {
            for (EquipmentSlot slot : ARMOR) {
                ItemStack s = mc.player.getEquippedStack(slot);
                if (!s.isEmpty()) {
                    stacks.add(s);
                    armorCount++;
                }
            }
            if (cfg.armorHeld) {
                ItemStack s = mc.player.getEquippedStack(EquipmentSlot.MAINHAND);
                if (!s.isEmpty()) {
                    stacks.add(s);
                }
            }
            if (cfg.armorOffhand) {
                ItemStack s = mc.player.getEquippedStack(EquipmentSlot.OFFHAND);
                if (!s.isEmpty()) {
                    stacks.add(s);
                }
            }
        }

        int count = stacks.size();
        if (count == 0) {
            w = 0;
            h = 0;
            return;
        }
        boolean twoGroups = armorCount > 0 && armorCount < count;
        int extra = twoGroups ? GROUP_GAP - GAP : 0;
        if (vertical) {
            labelW = 0;
            for (ItemStack s : stacks) {
                labelW = Math.max(labelW, mc.textRenderer.getWidth(durabilityText(s, cfg)));
            }
            w = SLOT + (labelW > 0 ? 4 + labelW : 0);
            h = count * SLOT + (count - 1) * GAP + extra;
        } else {
            w = count * SLOT + (count - 1) * GAP + extra;
            h = SLOT + (cfg.armorDurability != 0 ? 11 : 0);
        }
    }

    @Override
    public int width() {
        return w;
    }

    @Override
    public int height() {
        return h;
    }

    @Override
    public void render(DrawContext c, MinecraftClient mc, int x, int y, boolean preview) {
        int cx = x;
        int cy = y;
        for (int i = 0; i < stacks.size(); i++) {
            if (i == armorCount && armorCount > 0) {
                if (vertical) {
                    cy += GROUP_GAP - GAP;
                } else {
                    cx += GROUP_GAP - GAP;
                }
            }
            drawSlot(c, mc, stacks.get(i), cx, cy);
            if (vertical) {
                cy += SLOT + GAP;
            } else {
                cx += SLOT + GAP;
            }
        }
    }

    private void drawSlot(DrawContext c, MinecraftClient mc, ItemStack stack, int x, int y) {
        ModConfig cfg = ModConfig.get();
        boolean damageable = stack.getMaxDamage() > 0;
        int percent = damageable ? (stack.getMaxDamage() - stack.getDamage()) * 100 / stack.getMaxDamage() : 100;
        boolean low = cfg.armorLowWarning && damageable && percent <= cfg.armorLowPercent;

        int border = Ui.BORDER;
        if (low) {
            border = Ui.blink() ? Ui.BAD : 0xFF7A2B2B;
        }
        Ui.roundRect(c, x, y, SLOT, SLOT, low && Ui.blink() ? 0xB0401818 : Ui.BG);
        Ui.roundBorder(c, x, y, SLOT, SLOT, border);

        c.drawItem(stack, x + 3, y + 3);
        c.drawStackOverlay(mc.textRenderer, stack, x + 3, y + 3);

        String text = durabilityText(stack, cfg);
        if (!text.isEmpty()) {
            int color = low ? (Ui.blink() ? Ui.BAD : 0xFFB04040) : percent <= 50 ? Ui.WARN : Ui.MUTED;
            if (vertical) {
                c.drawTextWithShadow(mc.textRenderer, text, x + SLOT + 4, y + (SLOT - 8) / 2, color);
            } else {
                int tw = mc.textRenderer.getWidth(text);
                c.drawTextWithShadow(mc.textRenderer, text, x + (SLOT - tw) / 2, y + SLOT + 2, color);
            }
        }
    }
}
