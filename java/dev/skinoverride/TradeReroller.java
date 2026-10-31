package dev.skinoverride;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Villager trade reroller (client side). Pick an enchantment, a minimum level and a maximum price. The mod then
 * repeats, by itself: open the villager's trades, check them, and if they are not what you want, break the villager's
 * job block, put it back, wait for the villager to take the job again, and look at the new trades. It stops as soon
 * as a trade matches, with the trade screen open and ready.
 * <p>
 * It only works on a villager you have not traded with yet (trades lock after the first trade). Stand within 3
 * blocks of the villager and keep the job block (for example a lectern) in your hotbar. It stops if you open a menu,
 * get hurt, or press the reroll key again. Many servers forbid automation: use it only where it is allowed.
 */
public final class TradeReroller {

    private enum State {
        OFF, INTERACT, WAIT_SCREEN, CHECK, WAIT_CYCLE, BREAK, WAIT_PICKUP, PLACE, WAIT_JOB
    }

    private static final Set<Block> JOB_BLOCKS = Set.of(
            Blocks.LECTERN, Blocks.COMPOSTER, Blocks.SMITHING_TABLE, Blocks.GRINDSTONE, Blocks.FLETCHING_TABLE,
            Blocks.CARTOGRAPHY_TABLE, Blocks.STONECUTTER, Blocks.LOOM, Blocks.BREWING_STAND, Blocks.BLAST_FURNACE,
            Blocks.SMOKER, Blocks.BARREL, Blocks.CAULDRON);

    private static State state = State.OFF;
    private static VillagerEntity villager;
    private static VillagerEntity lastVillager;
    private static BlockPos jobPos;
    private static Block jobBlock;
    private static int toolSlot;
    private static int timer;
    private static int rolls;
    private static int interactFails;
    private static int sameCount;
    private static int breakWait = 25;
    private static String lastSig = "";
    private static String status = "";
    /** true = use the Trade Cycling mod's cycle key instead of breaking the job block */
    private static boolean useCycling;
    /** in automatic mode: fall back to breaking the job block if cycling does nothing */
    private static boolean canFallBack;
    private static int cycleFails;

    private TradeReroller() {
    }

    // ---------- setup ----------

    /** Remembers the villager you last right-clicked, so the Auto-reroll button in the trade screen knows it. */
    public static void registerCallbacks() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() && entity instanceof VillagerEntity v) {
                lastVillager = v;
            }
            return ActionResult.PASS;
        });
    }

    public static boolean active() {
        return state != State.OFF;
    }

    public static String status() {
        return status;
    }

    public static VillagerEntity lastVillager() {
        return lastVillager;
    }

    /** Adds "Auto-reroll" and "Setup" buttons above the trade screen. */
    public static void onScreenInit(MinecraftClient mc, Screen screen) {
        if (!(screen instanceof MerchantScreen)) {
            return;
        }
        int x0 = (screen.width - 276) / 2;
        int y = Math.max(2, (screen.height - 166) / 2 - 16);
        List<ClickableWidget> widgets = Screens.getButtons(screen);
        widgets.add(ButtonWidget.builder(Text.literal("Auto-reroll"), b -> {
            if (active()) {
                stop(mc, "Reroll stopped");
                return;
            }
            String error = start(mc, null);
            if (error != null) {
                mc.inGameHud.setOverlayMessage(Text.literal(error), false);
            }
        }).dimensions(x0, y, 62, 14).build());
        widgets.add(ButtonWidget.builder(Text.literal("Setup"), b -> mc.setScreen(new RerollScreen(null)))
                .dimensions(x0 + 64, y, 44, 14).build());
    }

    private static String prettyName(String path) {
        StringBuilder sb = new StringBuilder();
        for (String part : path.split("_")) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    /** Starts rerolling. Returns null if it started, otherwise a message saying what is missing. */
    public static String start(MinecraftClient mc, VillagerEntity target) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return "Join a world first";
        }
        ModConfig cfg = ModConfig.get();
        if (cfg.rerollEnchant == null || cfg.rerollEnchant.isBlank()) {
            return "Pick an enchantment first (Reroll setup)";
        }
        VillagerEntity v = target;
        if (v == null && lastVillager != null && lastVillager.isAlive() && !lastVillager.isRemoved()
                && mc.player.squaredDistanceTo(lastVillager) <= 36) {
            v = lastVillager;
        }
        if (v == null) {
            double best = 37;
            for (VillagerEntity other : mc.world.getEntitiesByClass(VillagerEntity.class,
                    mc.player.getBoundingBox().expand(6), e -> e.isAlive())) {
                double d = mc.player.squaredDistanceTo(other);
                if (d < best) {
                    best = d;
                    v = other;
                }
            }
        }
        if (v == null) {
            return "No villager within 6 blocks. Look at one and try again";
        }
        if (mc.player.squaredDistanceTo(v) > 36) {
            return "Stand closer to the villager";
        }

        int method = cfg.rerollMethod;
        boolean modLoaded = cyclingModLoaded();
        if (method == 1 && !modLoaded) {
            return "The Trade Cycling mod is not installed (pick another method)";
        }
        boolean cycling = method == 1 || (method == 0 && modLoaded);

        // the villager's job block: the closest one around it (needed when breaking and replacing it)
        BlockPos bestPos = null;
        Block block = null;
        BlockPos vp = v.getBlockPos();
        double bestScore = Double.MAX_VALUE;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockPos pos = vp.add(dx, dy, dz);
                    Block candidate = mc.world.getBlockState(pos).getBlock();
                    if (!JOB_BLOCKS.contains(candidate)) {
                        continue;
                    }
                    double score = pos.getSquaredDistance(vp) - (candidate == Blocks.LECTERN ? 1000 : 0);
                    if (score < bestScore) {
                        bestScore = score;
                        bestPos = pos;
                        block = candidate;
                    }
                }
            }
        }
        boolean breakPossible = bestPos != null && findHotbarSlot(mc, block.asItem()) >= 0;
        if (!cycling) {
            if (bestPos == null) {
                return "No job block (for example a lectern) found next to the villager";
            }
            if (!breakPossible) {
                return "Put a " + prettyName(Registries.ITEM.getId(block.asItem()).getPath()) + " in your hotbar so it can be placed back";
            }
        }
        useCycling = cycling;
        canFallBack = cycling && method == 0 && breakPossible;
        cycleFails = 0;

        villager = v;
        lastVillager = v;
        jobPos = bestPos;
        jobBlock = block;
        toolSlot = mc.player.getInventory().getSelectedSlot();
        rolls = 0;
        interactFails = 0;
        sameCount = 0;
        breakWait = 25;
        lastSig = "";
        timer = 0;
        state = mc.currentScreen instanceof MerchantScreen ? State.CHECK : State.INTERACT;
        status = "Starting...";
        mc.inGameHud.setOverlayMessage(Text.literal("Rerolling for " + wantedText(cfg) + " (press the reroll key to stop)"), false);
        return null;
    }

    private static String wantedText(ModConfig cfg) {
        return prettyName(cfg.rerollEnchant.replace("minecraft:", "")) + " " + cfg.rerollMinLevel + "+ for max "
                + cfg.rerollMaxCost + " emeralds";
    }

    public static void stop(MinecraftClient mc, String message) {
        if (state == State.BREAK && mc.interactionManager != null) {
            mc.interactionManager.cancelBlockBreaking();
        }
        state = State.OFF;
        status = message;
        if (mc.player != null) {
            mc.inGameHud.setOverlayMessage(Text.literal(message), false);
        }
    }

    // ---------- the loop ----------

    private static int findHotbarSlot(MinecraftClient mc, Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) {
                return i;
            }
        }
        return -1;
    }

    private static void selectTool(MinecraftClient mc) {
        if (mc.player.getInventory().getSelectedSlot() != toolSlot) {
            mc.player.getInventory().setSelectedSlot(toolSlot);
        }
    }

    public static void tick(MinecraftClient mc) {
        if (state == State.OFF) {
            return;
        }
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            state = State.OFF;
            return;
        }
        ModConfig cfg = ModConfig.get();
        if (villager == null || villager.isRemoved() || !villager.isAlive()) {
            stop(mc, "Reroll stopped: the villager is gone");
            return;
        }
        if (mc.player.hurtTime > 0) {
            stop(mc, "Reroll stopped: you were hurt");
            return;
        }
        if (mc.currentScreen != null && !(mc.currentScreen instanceof MerchantScreen)) {
            stop(mc, "Reroll stopped: a menu was opened");
            return;
        }
        if (rolls >= cfg.rerollMaxTries) {
            stop(mc, "Reroll stopped after " + rolls + " tries");
            return;
        }

        switch (state) {
            case INTERACT -> {
                if (mc.currentScreen instanceof MerchantScreen) {
                    state = State.WAIT_SCREEN;
                    timer = 0;
                    return;
                }
                if (mc.player.squaredDistanceTo(villager) > 12.25) {
                    stop(mc, "Reroll stopped: stand within 3 blocks of the villager");
                    return;
                }
                selectTool(mc);
                mc.interactionManager.interactEntity(mc.player, villager, Hand.MAIN_HAND);
                state = State.WAIT_SCREEN;
                timer = 0;
            }
            case WAIT_SCREEN -> {
                if (mc.currentScreen instanceof MerchantScreen) {
                    state = State.CHECK;
                    timer = 0;
                    return;
                }
                if (++timer >= 15) {
                    interactFails++;
                    if (interactFails > 30) {
                        stop(mc, "Reroll stopped: the villager never opened its trades (did it get a job?)");
                        return;
                    }
                    state = State.INTERACT;
                }
            }
            case CHECK -> check(mc, cfg);
            case WAIT_CYCLE -> waitCycle(mc);
            case BREAK -> {
                BlockState bs = mc.world.getBlockState(jobPos);
                if (!bs.isOf(jobBlock)) {
                    mc.interactionManager.cancelBlockBreaking();
                    state = State.WAIT_PICKUP;
                    timer = 0;
                    return;
                }
                if (mc.player.squaredDistanceTo(Vec3d.ofCenter(jobPos)) > 20.0) {
                    stop(mc, "Reroll stopped: too far from the job block");
                    return;
                }
                selectTool(mc);
                mc.interactionManager.updateBlockBreakingProgress(jobPos, Direction.UP);
                mc.player.swingHand(Hand.MAIN_HAND);
                if (++timer > 400) {
                    stop(mc, "Reroll stopped: could not break the job block");
                }
            }
            case WAIT_PICKUP -> {
                timer++;
                boolean haveItem = mc.player.isCreative() || findHotbarSlot(mc, jobBlock.asItem()) >= 0;
                if (haveItem) {
                    // wait long enough for the villager to notice its job block is gone
                    if (timer >= breakWait) {
                        state = State.PLACE;
                        timer = 0;
                    }
                } else if (timer > 100) {
                    stop(mc, "Reroll stopped: the job block did not come back to your hotbar");
                }
            }
            case PLACE -> place(mc);
            case WAIT_JOB -> {
                if (++timer >= 20) {
                    state = State.INTERACT;
                    interactFails = 0;
                }
            }
            default -> {
            }
        }
    }

    private static void place(MinecraftClient mc) {
        int slot = findHotbarSlot(mc, jobBlock.asItem());
        if (slot < 0) {
            stop(mc, "Reroll stopped: the job block is not in your hotbar");
            return;
        }
        mc.player.getInventory().setSelectedSlot(slot);
        BlockHitResult hit = null;
        for (Direction d : new Direction[]{Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST,
                Direction.WEST, Direction.UP}) {
            BlockPos neighbor = jobPos.offset(d);
            BlockState ns = mc.world.getBlockState(neighbor);
            if (ns.isAir() || !ns.getFluidState().isEmpty() || !ns.isSolidBlock(mc.world, neighbor)) {
                continue;
            }
            Direction face = d.getOpposite();
            hit = new BlockHitResult(Vec3d.ofCenter(neighbor).offset(face, 0.5), face, neighbor, false);
            break;
        }
        if (hit == null) {
            stop(mc, "Reroll stopped: nothing solid next to the job spot to place against");
            return;
        }
        ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        if (result.isAccepted()) {
            mc.player.swingHand(Hand.MAIN_HAND);
            state = State.WAIT_JOB;
            timer = 0;
        } else if (++timer > 20) {
            stop(mc, "Reroll stopped: could not place the job block");
        }
    }

    private static void check(MinecraftClient mc, ModConfig cfg) {
        if (!(mc.currentScreen instanceof MerchantScreen screen)) {
            state = State.INTERACT;
            return;
        }
        TradeOfferList offers = screen.getScreenHandler().getRecipes();
        if (offers == null || offers.isEmpty()) {
            if (++timer > 20) {
                mc.player.closeHandledScreen();
                state = State.INTERACT;
            }
            return;
        }
        rolls++;
        StringBuilder sig = new StringBuilder();
        for (TradeOffer offer : offers) {
            String hit = matchText(offer, cfg);
            if (hit != null) {
                state = State.OFF;
                status = "Found " + hit;
                mc.inGameHud.setOverlayMessage(Text.literal("Found " + hit + " after " + rolls + " tries"), false);
                mc.player.sendMessage(Text.literal("[Reroll] Found " + hit + " after " + rolls + " tries"), false);
                mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f));
                return;
            }
            sig.append(describe(offer)).append('|');
        }
        String now = sig.toString();
        if (useCycling) {
            lastSig = now;
            status = "Try " + rolls + ": no match yet (cycling)";
            mc.inGameHud.setOverlayMessage(Text.literal("Reroll try " + rolls + " of " + cfg.rerollMaxTries
                    + " (" + prettyName(cfg.rerollEnchant.replace("minecraft:", "")) + ")"), false);
            pressCycleKey(mc, screen, cfg);
            state = State.WAIT_CYCLE;
            timer = 0;
            return;
        }
        if (now.equals(lastSig)) {
            sameCount++;
            breakWait = Math.min(breakWait + 15, 140); // maybe the villager needs longer to lose its job
            if (sameCount >= 6) {
                stop(mc, "Reroll stopped: the trades never change. This villager is probably locked (already traded with)");
                return;
            }
        } else {
            sameCount = 0;
        }
        lastSig = now;
        status = "Try " + rolls + ": no match yet";
        mc.inGameHud.setOverlayMessage(Text.literal("Reroll try " + rolls + " of " + cfg.rerollMaxTries
                + " (" + prettyName(cfg.rerollEnchant.replace("minecraft:", "")) + ")"), false);

        mc.player.closeHandledScreen();
        state = State.BREAK;
        timer = 0;
    }

    // ---------- Trade Cycling mod ----------

    /** True if the Trade Cycling mod (it adds a "cycle trades" button and key to the trade screen) is installed. */
    public static boolean cyclingModLoaded() {
        for (net.fabricmc.loader.api.ModContainer mod : net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods()) {
            String id = mod.getMetadata().getId().toLowerCase(Locale.ROOT);
            if (id.contains("cycl") && id.contains("trade")) {
                return true;
            }
        }
        return false;
    }

    /** Presses the Trade Cycling key (C by default) as if you did, on the open trade screen. */
    private static void pressCycleKey(MinecraftClient mc, MerchantScreen screen, ModConfig cfg) {
        int key = cfg.rerollCycleKey;
        int scan = org.lwjgl.glfw.GLFW.glfwGetKeyScancode(key);
        screen.keyPressed(new net.minecraft.client.input.KeyInput(key, scan, 0));
    }

    private static String signature(TradeOfferList offers) {
        StringBuilder sb = new StringBuilder();
        if (offers != null) {
            for (TradeOffer offer : offers) {
                sb.append(describe(offer)).append('|');
            }
        }
        return sb.toString();
    }

    private static void waitCycle(MinecraftClient mc) {
        if (!(mc.currentScreen instanceof MerchantScreen screen)) {
            state = State.INTERACT;
            return;
        }
        String sig = signature(screen.getScreenHandler().getRecipes());
        if (!sig.isEmpty() && !sig.equals(lastSig)) {
            cycleFails = 0;
            state = State.CHECK;
            timer = 0;
            return;
        }
        if (++timer >= 12) {
            timer = 0;
            cycleFails++;
            if (cycleFails >= 5) {
                if (canFallBack) {
                    // the cycle key did nothing: use the job block method instead
                    useCycling = false;
                    canFallBack = false;
                    sameCount = 0;
                    breakWait = 25;
                    mc.inGameHud.setOverlayMessage(Text.literal("Trade Cycling did not respond, breaking the job block instead"), false);
                    mc.player.closeHandledScreen();
                    state = State.BREAK;
                } else {
                    stop(mc, "Reroll stopped: the trades did not change. Is the cycle key right (Reroll setup), "
                            + "is the villager already traded with, and is Trade Cycling on the server?");
                }
                return;
            }
            pressCycleKey(mc, screen, ModConfig.get());
        }
    }

    /** One line for the hover panel: "Mending I - 12 emeralds", or null if this trade is not an enchanted book. */
    public static String bookLine(TradeOffer offer) {
        ItemStack sell = offer.getSellItem();
        if (!sell.isOf(Items.ENCHANTED_BOOK)) {
            return null;
        }
        ItemEnchantmentsComponent comp = sell.get(DataComponentTypes.STORED_ENCHANTMENTS);
        if (comp == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (RegistryEntry<Enchantment> e : comp.getEnchantments()) {
            Optional<RegistryKey<Enchantment>> key = e.getKey();
            if (key.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(prettyName(key.get().getValue().getPath())).append(' ').append(roman(comp.getLevel(e)));
        }
        if (sb.length() == 0) {
            return null;
        }
        ItemStack first = offer.getDisplayedFirstBuyItem();
        sb.append(" - ").append(first.getCount()).append(first.isOf(Items.EMERALD) ? " emeralds" : " x " + first.getName().getString());
        return sb.toString();
    }

    private static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }

    // ---------- reading trades ----------

    private static String describe(TradeOffer offer) {
        ItemStack sell = offer.getSellItem();
        StringBuilder sb = new StringBuilder(Registries.ITEM.getId(sell.getItem()).toString());
        ItemEnchantmentsComponent comp = sell.get(DataComponentTypes.STORED_ENCHANTMENTS);
        if (comp != null) {
            for (RegistryEntry<Enchantment> e : comp.getEnchantments()) {
                Optional<RegistryKey<Enchantment>> key = e.getKey();
                sb.append(' ').append(key.isPresent() ? key.get().getValue().getPath() : "?").append(comp.getLevel(e));
            }
        }
        sb.append(" x").append(offer.getDisplayedFirstBuyItem().getCount());
        return sb.toString();
    }

    /** Returns a description if this trade is the enchanted book you asked for, otherwise null. */
    private static String matchText(TradeOffer offer, ModConfig cfg) {
        ItemStack sell = offer.getSellItem();
        if (!sell.isOf(Items.ENCHANTED_BOOK)) {
            return null;
        }
        ItemEnchantmentsComponent comp = sell.get(DataComponentTypes.STORED_ENCHANTMENTS);
        if (comp == null) {
            return null;
        }
        String want = cfg.rerollEnchant.toLowerCase(Locale.ROOT).replace("minecraft:", "").trim();
        ItemStack first = offer.getDisplayedFirstBuyItem();
        int cost = first.getCount();
        boolean priced = first.isOf(Items.EMERALD);
        for (RegistryEntry<Enchantment> e : comp.getEnchantments()) {
            Optional<RegistryKey<Enchantment>> key = e.getKey();
            if (key.isEmpty()) {
                continue;
            }
            String path = key.get().getValue().getPath();
            int level = comp.getLevel(e);
            if (path.equals(want) && level >= cfg.rerollMinLevel && (!priced || cost <= cfg.rerollMaxCost)) {
                return prettyName(path) + " " + level + (priced ? " for " + cost + " emeralds" : "");
            }
        }
        return null;
    }

    /** The villager-tradable enchantments, for the setup screen. */
    public static List<String> knownEnchants() {
        List<String> list = new ArrayList<>(List.of(
                "mending", "unbreaking", "efficiency", "fortune", "silk_touch", "sharpness", "smite",
                "bane_of_arthropods", "knockback", "fire_aspect", "looting", "sweeping_edge", "power", "punch",
                "flame", "infinity", "luck_of_the_sea", "lure", "loyalty", "impaling", "riptide", "channeling",
                "multishot", "quick_charge", "piercing", "protection", "fire_protection", "blast_protection",
                "projectile_protection", "feather_falling", "respiration", "aqua_affinity", "thorns",
                "depth_strider", "frost_walker", "binding_curse", "vanishing_curse", "soul_speed", "swift_sneak"));
        list.sort(String::compareTo);
        return list;
    }

    public static String pretty(String path) {
        return prettyName(path);
    }
}
