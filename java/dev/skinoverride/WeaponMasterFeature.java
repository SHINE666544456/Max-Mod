package dev.skinoverride;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.RotationAxis;

import java.util.HashMap;
import java.util.Map;

/**
 * Weapon Master: the weapons, tools, bows, shields and banners in your hotbar are drawn on your own
 * character (swords on the hips, the rest on the back). Other players with the mod (and a server with the mod)
 * see it too, and you see theirs. It shows up in third person and freecam.
 */
public class WeaponMasterFeature extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    private enum Kind {
        SWORD, TOOL, TRIDENT, BOW, SHIELD, BANNER
    }

    /** One set of render states per player, so two players drawn in the same frame never share one. */
    private final Map<Integer, ItemRenderState[]> stateCache = new HashMap<>();
    private boolean failed;

    public WeaponMasterFeature(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    private ItemRenderState[] statesFor(int entityId) {
        if (stateCache.size() > 48) {
            stateCache.clear();
        }
        return stateCache.computeIfAbsent(entityId, id -> {
            ItemRenderState[] arr = new ItemRenderState[9];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new ItemRenderState();
            }
            return arr;
        });
    }

    /** True for the kinds of item Weapon Master draws (also used to check what other players send). */
    public static boolean isWeapon(ItemStack stack) {
        return classify(stack) != null;
    }

    private static Kind classify(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        String path = Registries.ITEM.getId(stack.getItem()).getPath();
        if (path.endsWith("sword")) {
            return Kind.SWORD;
        }
        if (path.endsWith("_axe") || path.endsWith("pickaxe") || path.endsWith("_shovel") || path.endsWith("_hoe")
                || path.equals("mace")) {
            return Kind.TOOL;
        }
        if (path.equals("trident")) {
            return Kind.TRIDENT;
        }
        if (path.equals("bow") || path.equals("crossbow")) {
            return Kind.BOW;
        }
        if (path.endsWith("shield")) {
            return Kind.SHIELD;
        }
        if (path.endsWith("_banner")) {
            return Kind.BANNER;
        }
        return null;
    }

    private static boolean allowed(Kind kind, ModConfig cfg) {
        return switch (kind) {
            case SWORD -> cfg.wmSwords;
            case TOOL -> cfg.wmTools;
            case TRIDENT -> cfg.wmTrident;
            case BOW -> cfg.wmBows;
            case SHIELD -> cfg.wmShield;
            case BANNER -> cfg.wmBanner;
        };
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                       PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        ModConfig cfg = ModConfig.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (failed || mc.player == null || mc.world == null || state.invisible || state.spectator) {
            return;
        }
        Entity entity = mc.world.getEntityById(state.id);
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        ItemStack[] stacks;
        int selected;
        if (state.id == mc.player.getId()) {
            if (!cfg.wmOn) {
                return;
            }
            PlayerInventory inventory = mc.player.getInventory();
            stacks = new ItemStack[9];
            for (int i = 0; i < 9; i++) {
                stacks[i] = inventory.getStack(i);
            }
            selected = inventory.getSelectedSlot();
        } else {
            // Other players: only if they have the mod and share their loadout (server needs the mod too).
            if (!cfg.wmShowOthers) {
                return;
            }
            Cosmetics.Remote remote = Cosmetics.remote(entity.getUuid());
            if (remote == null || remote.stacks == null) {
                return;
            }
            stacks = remote.stacks;
            selected = remote.selected;
        }
        try {
            draw(matrices, queue, light, mc, cfg, living, stacks, selected, statesFor(state.id));
        } catch (RuntimeException e) {
            failed = true; // never take the game down over a cosmetic feature
            SkinOverrideClient.LOGGER.warn("Weapon Master disabled after an error", e);
        }
    }

    private void draw(MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                      MinecraftClient mc, ModConfig cfg, LivingEntity living, ItemStack[] stacks, int selected,
                      ItemRenderState[] states) {
        PlayerEntityModel model = this.getContextModel();
        float k = cfg.wmScale / 100f;

        int swords = 0;
        int tools = 0;
        boolean trident = false;
        boolean bow = false;
        boolean shield = false;
        boolean banner = false;

        for (int slot = 0; slot < 9; slot++) {
            if (slot == selected) {
                continue; // the item in your hand is already in your hand
            }
            ItemStack stack = stacks[slot];
            Kind kind = classify(stack);
            if (kind == null || !allowed(kind, cfg)) {
                continue;
            }
            // positions (model space: x+ = your left, y+ = down, z+ = your back), tilt, size
            float x;
            float y;
            float z;
            float tilt;
            float size;
            switch (kind) {
                case SWORD -> {
                    if (swords >= 2) {
                        continue;
                    }
                    x = swords == 0 ? 0.33f : -0.33f;
                    y = 0.64f;
                    z = 0.0f;
                    tilt = -135f;
                    size = 1.4f;
                    swords++;
                }
                case TOOL -> {
                    if (tools >= 2) {
                        continue;
                    }
                    x = 0.0f;
                    y = 0.32f;
                    z = 0.20f + tools * 0.02f;
                    tilt = tools == 0 ? 30f : 60f;
                    size = 1.3f;
                    tools++;
                }
                case TRIDENT -> {
                    if (trident) {
                        continue;
                    }
                    x = 0.12f;
                    y = 0.30f;
                    z = 0.19f;
                    tilt = 45f;
                    size = 1.3f;
                    trident = true;
                }
                case BOW -> {
                    if (bow) {
                        continue;
                    }
                    x = 0.0f;
                    y = 0.40f;
                    z = 0.24f;
                    tilt = 0f;
                    size = 1.4f;
                    bow = true;
                }
                case SHIELD -> {
                    if (shield) {
                        continue;
                    }
                    x = 0.0f;
                    y = 0.33f;
                    z = 0.27f;
                    tilt = 0f;
                    size = 1.6f;
                    shield = true;
                }
                default -> {
                    if (banner) {
                        continue;
                    }
                    x = 0.0f;
                    y = 0.55f;
                    z = 0.23f;
                    tilt = 0f;
                    size = 1.0f;
                    banner = true;
                }
            }

            ItemRenderState render = states[slot];
            render.clear();
            mc.getItemModelManager().updateForLivingEntity(render, stack, ItemDisplayContext.FIXED, living);
            if (render.isEmpty()) {
                continue;
            }
            matrices.push();
            model.body.applyTransform(matrices);
            matrices.translate(x, y, z);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F)); // undo the renderer's upside-down space
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(tilt));
            float s = size * k;
            matrices.scale(s, s, s);
            render.render(matrices, queue, light, OverlayTexture.DEFAULT_UV, 0);
            matrices.pop();
        }
    }
}
