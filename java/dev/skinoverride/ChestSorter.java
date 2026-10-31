package dev.skinoverride;

import dev.skinoverride.mixin.HandledScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.Generic3x3ContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.HopperScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Press the sort key (default R) in a chest, barrel, ender chest, shulker box, dispenser, hopper or your
 * own inventory to merge stacks and sort the items by their ID. Works on whatever inventory your mouse is over
 * (the container if the mouse is elsewhere). Everything is done with ordinary slot clicks.
 */
public final class ChestSorter {

    private static KeyBinding key;
    private static boolean wasDown;

    private ChestSorter() {
    }

    public static void setKey(KeyBinding binding) {
        key = binding;
    }

    /** Called every client tick; reacts to a fresh key press while a supported screen is open. */
    public static void tick(MinecraftClient mc) {
        autoTick(mc);
        if (key == null) {
            return;
        }
        boolean down = mc.currentScreen != null && PhysicalKeys.isDown(mc, key);
        boolean pressed = down && !wasDown;
        wasDown = down;
        if (!pressed || !ModConfig.get().sorterOn || mc.player == null || mc.interactionManager == null) {
            return;
        }
        Screen screen = mc.currentScreen;
        if (!(screen instanceof HandledScreen<?> handled) || !supported(screen)) {
            return;
        }
        sort(mc, handled, null);
    }

    public static boolean supported(Screen screen) {
        return screen instanceof GenericContainerScreen
                || screen instanceof ShulkerBoxScreen
                || screen instanceof Generic3x3ContainerScreen
                || screen instanceof HopperScreen
                || screen instanceof InventoryScreen;
    }

    private static boolean isPlayerInventory(Slot slot) {
        return slot.inventory instanceof PlayerInventory;
    }

    /** Sorts the chest / container part of the open screen. */
    public static void sortContainer(MinecraftClient mc, HandledScreen<?> screen) {
        if (mc.player != null && mc.interactionManager != null) {
            sort(mc, screen, false);
        }
    }

    /** Sorts your own inventory (works from any supported screen). */
    public static void sortInventory(MinecraftClient mc, HandledScreen<?> screen) {
        if (mc.player != null && mc.interactionManager != null) {
            sort(mc, screen, true);
        }
    }

    /** forcePlayer: null = decide by mouse position, true = your inventory, false = the container. */
    private static void sort(MinecraftClient mc, HandledScreen<?> screen, Boolean forcePlayer) {
        ScreenHandler handler = screen.getScreenHandler();
        if (!handler.getCursorStack().isEmpty()) {
            return; // never sort while holding an item on the cursor
        }
        boolean hotbar = ModConfig.get().sorterHotbar;
        Slot hovered = ((HandledScreenAccessor) (Object) screen).skinoverride$getFocusedSlot();

        // Decide which inventory to sort: the one under the mouse, else the container, else your inventory.
        boolean sortPlayer;
        if (screen instanceof InventoryScreen) {
            sortPlayer = true; // the only sortable inventory there is your own
        } else if (forcePlayer != null) {
            sortPlayer = forcePlayer;
        } else if (hovered != null) {
            sortPlayer = isPlayerInventory(hovered);
        } else {
            sortPlayer = true;
            for (Slot s : handler.slots) {
                if (!isPlayerInventory(s)) {
                    sortPlayer = false;
                    break;
                }
            }
        }

        List<Integer> ids = new ArrayList<>(); // slot ids (positions in the screen handler)
        for (Slot s : handler.slots) {
            boolean player = isPlayerInventory(s);
            if (player != sortPlayer) {
                continue;
            }
            if (player) {
                int index = s.getIndex();
                boolean main = index >= 9 && index < 36;
                boolean bar = index >= 0 && index < 9;
                if (!(main || (bar && hotbar))) {
                    continue; // skip armor, offhand, crafting
                }
            }
            ids.add(s.id);
        }
        if (ids.size() < 2) {
            return;
        }
        // keep a stable top-to-bottom order of slots (hotbar goes last in handler order already)
        ids.sort(Comparator.naturalOrder());

        mergeStacks(mc, handler, ids);
        sortStacks(mc, handler, ids);
    }


    // ---------- quick deposit ("stash") ----------

    /**
     * Shift-clicks the items of your inventory into the open container.
     * matchingOnly = only items the container already holds (so a chest of cobblestone only gets cobblestone).
     */
    public static void deposit(MinecraftClient mc, HandledScreen<?> screen, boolean matchingOnly) {
        if (mc.player == null || mc.interactionManager == null || screen instanceof InventoryScreen) {
            return;
        }
        ScreenHandler handler = screen.getScreenHandler();
        if (!handler.getCursorStack().isEmpty()) {
            return;
        }
        boolean hotbar = ModConfig.get().sorterHotbar;
        java.util.Set<net.minecraft.item.Item> inside = new java.util.HashSet<>();
        for (Slot s : handler.slots) {
            if (!isPlayerInventory(s) && !s.getStack().isEmpty()) {
                inside.add(s.getStack().getItem());
            }
        }
        for (Slot s : new ArrayList<>(handler.slots)) {
            if (!isPlayerInventory(s)) {
                continue;
            }
            int index = s.getIndex();
            boolean main = index >= 9 && index < 36;
            boolean bar = index >= 0 && index < 9;
            if (!(main || (bar && hotbar))) {
                continue;
            }
            ItemStack stack = s.getStack();
            if (stack.isEmpty() || (matchingOnly && !inside.contains(stack.getItem()))) {
                continue;
            }
            mc.interactionManager.clickSlot(handler.syncId, s.id, 0, SlotActionType.QUICK_MOVE, mc.player);
        }
    }

    // ---------- on-screen buttons and auto sort ----------

    private static Screen lastAutoScreen;
    private static Screen autoScreen;
    private static int autoTicks;

    private static int backgroundHeight(HandledScreen<?> screen) {
        if (screen.getScreenHandler() instanceof GenericContainerScreenHandler g) {
            return 114 + g.getRows() * 18;
        }
        if (screen instanceof HopperScreen) {
            return 133;
        }
        return 166;
    }

    /** Called when a screen opens: adds the Sort / Stash buttons and schedules an automatic sort. */
    public static void onScreenInit(MinecraftClient mc, Screen screen) {
        if (!(screen instanceof HandledScreen<?> handled) || !supported(screen)) {
            return;
        }
        ModConfig cfg = ModConfig.get();
        boolean inventory = screen instanceof InventoryScreen;

        if (cfg.sorterButtons) {
            int x0 = (screen.width - 176) / 2;
            int y0 = (screen.height - backgroundHeight(handled)) / 2;
            int y = Math.max(2, y0 - 16);
            List<ClickableWidget> widgets = Screens.getButtons(screen);
            if (inventory) {
                widgets.add(ButtonWidget.builder(Text.literal("Sort"), b -> sortInventory(mc, handled))
                        .dimensions(x0, y, 40, 14).build());
            } else {
                widgets.add(ButtonWidget.builder(Text.literal("Sort"), b -> sortContainer(mc, handled))
                        .dimensions(x0, y, 36, 14).build());
                widgets.add(ButtonWidget.builder(Text.literal("Stash"), b -> deposit(mc, handled, false))
                        .dimensions(x0 + 38, y, 40, 14).build());
                widgets.add(ButtonWidget.builder(Text.literal("Same"), b -> deposit(mc, handled, true))
                        .dimensions(x0 + 80, y, 36, 14).build());
                widgets.add(ButtonWidget.builder(Text.literal("My inv"), b -> sortInventory(mc, handled))
                        .dimensions(x0 + 118, y, 44, 14).build());
            }
        }

        boolean wantAuto = inventory ? cfg.sorterAutoPlayer : (cfg.sorterAuto || cfg.sorterAutoPlayer);
        if (wantAuto && screen != lastAutoScreen) {
            lastAutoScreen = screen;
            autoScreen = screen;
            autoTicks = 0;
        }
    }

    private static boolean containerHasItems(ScreenHandler handler) {
        for (Slot s : handler.slots) {
            if (!isPlayerInventory(s) && !s.getStack().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void autoTick(MinecraftClient mc) {
        if (autoScreen == null) {
            return;
        }
        if (mc.currentScreen != autoScreen || mc.player == null) {
            autoScreen = null;
            return;
        }
        autoTicks++;
        HandledScreen<?> handled = (HandledScreen<?>) autoScreen;
        boolean inventory = autoScreen instanceof InventoryScreen;
        // the chest contents arrive a moment after the screen opens: wait for them (but not forever)
        boolean ready = inventory ? autoTicks >= 2 : (containerHasItems(handled.getScreenHandler()) ? autoTicks >= 3 : autoTicks >= 20);
        if (!ready) {
            return;
        }
        Screen done = autoScreen;
        autoScreen = null;
        ModConfig cfg = ModConfig.get();
        if (!inventory && cfg.sorterAuto) {
            sortContainer(mc, handled);
        }
        if (cfg.sorterAutoPlayer && mc.currentScreen == done) {
            sortInventory(mc, handled);
        }
    }

    // ---------- step 1: merge partial stacks ----------

    private static void click(MinecraftClient mc, ScreenHandler handler, int slotId) {
        mc.interactionManager.clickSlot(handler.syncId, slotId, 0, SlotActionType.PICKUP, mc.player);
    }

    private static boolean sameKind(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && ItemStack.areItemsAndComponentsEqual(a, b);
    }

    private static void mergeStacks(MinecraftClient mc, ScreenHandler handler, List<Integer> ids) {
        for (int i = 0; i < ids.size(); i++) {
            int a = ids.get(i);
            for (int j = i + 1; j < ids.size(); j++) {
                ItemStack target = handler.getSlot(a).getStack();
                if (target.isEmpty() || target.getCount() >= target.getMaxCount()) {
                    break;
                }
                int b = ids.get(j);
                ItemStack source = handler.getSlot(b).getStack();
                if (!sameKind(target, source)) {
                    continue;
                }
                click(mc, handler, b);       // pick up the source stack
                click(mc, handler, a);       // drop it on the target: merges as much as fits
                if (!handler.getCursorStack().isEmpty()) {
                    click(mc, handler, b);   // put the remainder back
                }
            }
        }
    }

    // ---------- step 2: order by item ID ----------

    private static String idOf(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).toString();
    }

    private static void sortStacks(MinecraftClient mc, ScreenHandler handler, List<Integer> ids) {
        int n = ids.size();
        // current contents (copies) so the plan can be simulated before sending any clicks
        List<ItemStack> sim = new ArrayList<>();
        for (int id : ids) {
            sim.add(handler.getSlot(id).getStack().copy());
        }
        Comparator<ItemStack> order = Comparator
                .comparing((ItemStack s) -> s.isEmpty() ? 1 : 0)       // empty slots last
                .thenComparing(s -> s.isEmpty() ? "" : idOf(s))
                .thenComparing(s -> -s.getCount());
        List<ItemStack> wanted = new ArrayList<>(sim);
        wanted.sort(order);

        for (int pos = 0; pos < n; pos++) {
            ItemStack want = wanted.get(pos);
            ItemStack have = sim.get(pos);
            if (same(want, have)) {
                continue;
            }
            // find where the wanted stack currently is (search from the end so already-placed slots stay)
            int from = -1;
            for (int k = pos + 1; k < n; k++) {
                if (same(sim.get(k), want)) {
                    from = k;
                    break;
                }
            }
            if (from < 0) {
                continue;
            }
            swap(mc, handler, ids.get(pos), ids.get(from), sim.get(pos).isEmpty());
            ItemStack tmp = sim.get(pos);
            sim.set(pos, sim.get(from));
            sim.set(from, tmp);
        }
    }

    /** Two slots are interchangeable if both are empty or hold the same kind of item (counts may differ). */
    private static boolean same(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) {
            return true;
        }
        return sameKind(a, b);
    }

    /** Swaps two slots with clicks: pick up A, click B (swaps with the cursor), put the result back into A. */
    private static void swap(MinecraftClient mc, ScreenHandler handler, int a, int b, boolean aIsEmpty) {
        if (aIsEmpty) {
            click(mc, handler, b);   // pick up from B
            click(mc, handler, a);   // drop into the empty slot A
            return;
        }
        click(mc, handler, a);
        click(mc, handler, b);
        click(mc, handler, a);
    }
}
