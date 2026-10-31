package dev.skinoverride.hud;

import dev.skinoverride.AfkTracker;
import dev.skinoverride.ModConfig;
import dev.skinoverride.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows what the player you are looking at is holding and wearing (the same data the server
 * already sends every client for rendering). Only players you can actually see are shown.
 */
public class InspectWidget implements HudWidget {

    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final int HOLD_MS = 600;
    private static final int MAX_LINE = 250;

    private record Line(String name, String enchants) {
    }

    private PlayerEntity target;
    private long targetSeenMs;

    private String title = "";
    private String afkText = "";
    private String hpText = "";
    private int hpColor = Ui.GOOD;
    private final List<ItemStack> stacks = new ArrayList<>();
    private final List<Line> lines = new ArrayList<>();
    private List<ItemStack> demo;
    private int w;
    private int h;

    @Override
    public String id() {
        return "inspect";
    }

    @Override
    public String label() {
        return "Crosshair inspect";
    }

    @Override
    public float defaultX() {
        return 0.62f;
    }

    @Override
    public float defaultY() {
        return 0.42f;
    }

    @Override
    public boolean enabled() {
        return ModConfig.get().inspectHud;
    }

    @Override
    public boolean visible(MinecraftClient mc) {
        return mc.currentScreen == null;
    }

    private PlayerEntity findTarget(MinecraftClient mc) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1.0f);
        double range = ModConfig.get().inspectRange;
        PlayerEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) {
                continue;
            }
            Vec3d to = p.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist > range || dist < 0.5) {
                continue;
            }
            double along = to.dotProduct(look);
            if (along <= 0) {
                continue;
            }
            double perp = to.subtract(look.multiply(along)).length();
            if (perp > 0.8 || !mc.player.canSee(p)) {
                continue;
            }
            if (dist < bestDist) {
                bestDist = dist;
                best = p;
            }
        }
        return best;
    }

    @Override
    public void prepare(MinecraftClient mc, boolean preview) {
        ModConfig cfg = ModConfig.get();
        stacks.clear();
        lines.clear();
        afkText = "";

        if (preview || mc.player == null || mc.world == null) {
            if (demo == null) {
                demo = List.of(new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.TOTEM_OF_UNDYING),
                        new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE),
                        new ItemStack(Items.DIAMOND_LEGGINGS), new ItemStack(Items.DIAMOND_BOOTS));
            }
            title = "Steve";
            hpText = "20/20";
            hpColor = Ui.GOOD;
            stacks.addAll(demo);
            lines.add(new Line("Diamond Sword", cfg.inspectEnchants ? "Sharpness V, Unbreaking III, Mending" : ""));
            lines.add(new Line("Diamond Helmet", cfg.inspectEnchants ? "Protection IV, Respiration III" : ""));
        } else {
            long now = Util.getMeasuringTimeMs();
            PlayerEntity found = findTarget(mc);
            if (found != null) {
                target = found;
                targetSeenMs = now;
            } else if (target != null && (now - targetSeenMs > HOLD_MS || target.isRemoved())) {
                target = null;
            }
            if (target == null) {
                w = 0;
                h = 0;
                return;
            }
            title = target.getName().getString();
            if (AfkTracker.isAfk(target.getUuid())) {
                afkText = "AFK " + Ui.formatShort(AfkTracker.idleSeconds(target.getUuid()));
            }
            float hp = target.getHealth();
            float max = Math.max(1f, target.getMaxHealth());
            hpText = (int) Math.ceil(hp) + "/" + (int) Math.ceil(max);
            hpColor = hp / max >= 0.6f ? Ui.GOOD : hp / max >= 0.3f ? Ui.WARN : Ui.BAD;
            for (EquipmentSlot slot : SLOTS) {
                ItemStack s = target.getEquippedStack(slot);
                stacks.add(s);
                if (!s.isEmpty()) {
                    lines.add(new Line(s.getName().getString(), cfg.inspectEnchants ? enchantText(s) : ""));
                }
            }
        }

        var tr = mc.textRenderer;
        int width = 5 + 6 * 22 - 2 + 5;
        int headerW = 10 + tr.getWidth(title) + (afkText.isEmpty() ? 0 : 8 + tr.getWidth(afkText))
                + (cfg.inspectHealth ? 14 + tr.getWidth(hpText) : 0);
        width = Math.max(width, headerW);
        for (Line l : lines) {
            int lw = 10 + tr.getWidth(l.name()) + (l.enchants().isEmpty() ? 0 : 8 + tr.getWidth(l.enchants()));
            width = Math.max(width, Math.min(lw, MAX_LINE));
        }
        w = width;
        h = 4 + 11 + 3 + 22 + (lines.isEmpty() ? 0 : 3 + lines.size() * 10) + 4;
    }

    private static String enchantText(ItemStack stack) {
        ItemEnchantmentsComponent comp = stack.getEnchantments();
        if (comp == null || comp.isEmpty()) {
            return "";
        }
        List<String> names = new ArrayList<>();
        for (RegistryEntry<Enchantment> e : comp.getEnchantments()) {
            names.add(Enchantment.getName(e, comp.getLevel(e)).getString());
        }
        return String.join(", ", names);
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
        var tr = mc.textRenderer;
        Ui.panel(c, x, y, w, h);
        c.fill(x + 1, y + 1, x + w - 1, y + 2, Ui.ACCENT_DIM);

        int ty = y + 5;
        c.drawTextWithShadow(tr, title, x + 5, ty, Ui.TEXT);
        if (!afkText.isEmpty()) {
            c.drawTextWithShadow(tr, afkText, x + 5 + tr.getWidth(title) + 8, ty, Ui.WARN);
        }
        if (ModConfig.get().inspectHealth) {
            c.drawTextWithShadow(tr, hpText, x + w - 5 - tr.getWidth(hpText), ty, hpColor);
        }

        int sy = y + 4 + 11 + 3;
        for (int i = 0; i < stacks.size(); i++) {
            int sx = x + 5 + i * 22;
            Ui.roundRect(c, sx, sy, 20, 20, Ui.BG_LIGHT);
            Ui.roundBorder(c, sx, sy, 20, 20, Ui.BORDER);
            ItemStack s = stacks.get(i);
            if (!s.isEmpty()) {
                c.drawItem(s, sx + 2, sy + 2);
                c.drawStackOverlay(tr, s, sx + 2, sy + 2);
            }
        }

        int ly = sy + 22 + 3;
        for (Line l : lines) {
            c.drawTextWithShadow(tr, l.name(), x + 5, ly, Ui.TEXT);
            if (!l.enchants().isEmpty()) {
                int startX = x + 5 + tr.getWidth(l.name()) + 8;
                int room = x + w - 5 - startX;
                if (room > 24) {
                    c.drawTextWithShadow(tr, tr.trimToWidth(l.enchants(), room), startX, ly, 0xFFB69CFF);
                }
            }
            ly += 10;
        }
    }
}
