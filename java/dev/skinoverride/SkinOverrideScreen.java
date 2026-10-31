package dev.skinoverride;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Left: players. Center: 3D preview + what they hold/wear. Right: your skin files.
 * Pick a player, click a skin. Everything is local to your client.
 */
public class SkinOverrideScreen extends ThemedScreen {

    private static final int ROW_H = 14;
    private static final int TOP = 36;

    // Yarn 1.21.11 has a duplicate PLAYER_SLIM constant that can't be referenced, so build the layers by hand.
    private static final EntityModelLayer PLAYER_LAYER =
            new EntityModelLayer(Identifier.ofVanilla("player"), "main");
    private static final EntityModelLayer PLAYER_SLIM_LAYER =
            new EntityModelLayer(Identifier.ofVanilla("player_slim"), "main");

    private static final EquipmentSlot[] INSPECT_SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final String[] INSPECT_NAMES = {
            "Main hand", "Off hand", "Head", "Chest", "Legs", "Feet"
    };

    private record PlayerRow(UUID id, String name) {
    }

    private final Screen parent;
    private final UUID preselect;

    private final List<PlayerRow> players = new ArrayList<>();
    private List<String> allSkins = new ArrayList<>();
    private final List<String> shownSkins = new ArrayList<>();

    private UUID selectedPlayer;
    private String skinQuery = "";
    private double playerScroll;
    private double skinScroll;
    private int refreshTimer;
    private int inspectSlot;
    private Text status = Text.empty();

    private TextFieldWidget searchField;
    private TextFieldWidget nameField;

    private PlayerEntityModel wideModel;
    private PlayerEntityModel slimModel;

    // computed in layout()
    private int lx, lw, cx, cw, rx, rw, bottom;

    public SkinOverrideScreen(Screen parent, UUID preselect) {
        super(Text.literal("IRIS'S Overrides and Tweaks"));
        this.parent = parent;
        this.preselect = preselect;
    }

    private void layout() {
        lw = MathHelper.clamp(this.width / 4, 110, 150);
        rw = MathHelper.clamp(this.width / 3, 130, 190);
        lx = 10;
        rx = this.width - 10 - rw;
        cx = lx + lw + 8;
        cw = rx - 8 - cx;
        bottom = this.height - 74;
    }

    private double maxScroll(int count) {
        return Math.max(0, count * ROW_H + 4 - (bottom - TOP));
    }

    // ---------- setup ----------

    @Override
    protected void build() {
        layout();

        LoadedEntityModels models = this.client.getLoadedEntityModels();
        this.wideModel = new PlayerEntityModel(models.getModelPart(PLAYER_LAYER), false);
        this.slimModel = new PlayerEntityModel(models.getModelPart(PLAYER_SLIM_LAYER), true);

        refreshPlayers();
        refreshSkins();
        if (selectedPlayer == null) {
            selectedPlayer = pickInitialPlayer();
        }

        this.searchField = new TextFieldWidget(this.textRenderer, rx, 18, rw, 14, Text.literal("Search skins"));
        this.searchField.setPlaceholder(Text.literal("Search skins...").formatted(Formatting.DARK_GRAY));
        this.searchField.setText(skinQuery);
        this.searchField.setChangedListener(text -> {
            skinQuery = text;
            skinScroll = 0;
            refreshSkins();
        });
        this.addDrawableChild(this.searchField);

        int g = 4;
        int y1 = this.height - 52;
        int fieldW = MathHelper.clamp((this.width - 20) * 3 / 10, 90, 160);
        int btnW = (this.width - 20 - fieldW - g * 3) / 3;
        this.nameField = new TextFieldWidget(this.textRenderer, 10, y1, fieldW, 20, Text.literal("Mojang username"));
        this.nameField.setPlaceholder(Text.literal("Mojang username").formatted(Formatting.DARK_GRAY));
        this.nameField.setMaxLength(16);
        this.addDrawableChild(this.nameField);

        button(10 + fieldW + g, y1, btnW, 20, () -> "Download skin", () -> !MojangLookup.isBusy(), true, this::onFetch);
        button(10 + fieldW + g + (btnW + g), y1, btnW, 20, "Open folder", false,
                () -> Util.getOperatingSystem().open(SkinStore.skinsDir().toFile()));
        button(10 + fieldW + g + (btnW + g) * 2, y1, btnW, 20, "Reload PNGs", false, () -> {
            SkinStore.reload();
            refreshSkins();
            setStatus(Text.literal("Reloaded skins from disk.").formatted(Formatting.GREEN));
        });

        int y2 = this.height - 28;
        int b2 = (this.width - 20 - g * 4) / 5;
        button(10, y2, b2, 20, () -> "Reset player",
                () -> selectedPlayer != null && SkinStore.assignedFile(selectedPlayer) != null, false, () -> {
                    SkinStore.clear(selectedPlayer);
                    setStatus(Text.literal("Back to the real skin.").formatted(Formatting.GREEN));
                });
        button(10 + (b2 + g), y2, b2, 20, () -> "Restore last",
                () -> selectedPlayer != null && SkinStore.assignedFile(selectedPlayer) == null
                        && SkinStore.lastFile(selectedPlayer) != null, false, () -> {
                    String error = SkinStore.restoreLast(selectedPlayer);
                    setStatus(error == null
                            ? Text.literal("Restored " + SkinStore.lastFile(selectedPlayer) + ".").formatted(Formatting.GREEN)
                            : Text.literal(error).formatted(Formatting.YELLOW));
                });
        button(10 + (b2 + g) * 2, y2, b2, 20,
                () -> "Model: " + (selectedPlayer == null ? "auto" : SkinStore.modelMode(selectedPlayer)),
                () -> selectedPlayer != null && SkinStore.assignedFile(selectedPlayer) != null, false,
                () -> SkinStore.cycleModel(selectedPlayer));
        button(10 + (b2 + g) * 3, y2, b2, 20, "Settings", false,
                () -> this.client.setScreen(new SettingsScreen(this)));
        button(10 + (b2 + g) * 4, y2, b2, 20, "Done", true, this::close);
    }

    private UUID pickInitialPlayer() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (preselect != null && hasPlayer(preselect)) {
            return preselect;
        }
        UUID last = SkinStore.lastPlayer();
        if (last != null && hasPlayer(last)) {
            return last;
        }
        if (mc.player != null && hasPlayer(mc.player.getUuid())) {
            return mc.player.getUuid();
        }
        return players.isEmpty() ? null : players.get(0).id();
    }

    private boolean hasPlayer(UUID id) {
        return players.stream().anyMatch(p -> p.id().equals(id));
    }

    private void refreshPlayers() {
        MinecraftClient mc = MinecraftClient.getInstance();
        players.clear();
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (handler != null) {
            for (PlayerListEntry entry : handler.getPlayerList()) {
                players.add(new PlayerRow(entry.getProfile().id(), entry.getProfile().name()));
            }
        }
        if (players.isEmpty() && mc.player != null) {
            players.add(new PlayerRow(mc.player.getUuid(), mc.player.getName().getString()));
        }
        UUID self = mc.player == null ? null : mc.player.getUuid();
        players.sort(Comparator
                .comparing((PlayerRow p) -> !p.id().equals(self))
                .thenComparing(p -> p.name().toLowerCase(Locale.ROOT)));
        if (selectedPlayer != null && !hasPlayer(selectedPlayer)) {
            selectedPlayer = null;
        }
    }

    private void refreshSkins() {
        allSkins = SkinStore.listSkins();
        shownSkins.clear();
        String needle = skinQuery.trim().toLowerCase(Locale.ROOT);
        for (String file : allSkins) {
            if (needle.isEmpty() || file.toLowerCase(Locale.ROOT).contains(needle)) {
                shownSkins.add(file);
            }
        }
        skinScroll = MathHelper.clamp(skinScroll, 0.0, maxScroll(shownSkins.size()));
    }

    private void setStatus(Text text) {
        this.status = text;
    }

    // ---------- actions ----------

    private void onFetch() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            setStatus(Text.literal("Type a Minecraft username first.").formatted(Formatting.YELLOW));
            return;
        }
        if (MojangLookup.isBusy()) {
            return;
        }
        setStatus(Text.literal("Downloading skin of " + name + "...").formatted(Formatting.GRAY));
        MojangLookup.fetch(name, result -> {
            if (result.error() != null) {
                setStatus(Text.literal(result.error()).formatted(Formatting.RED));
                return;
            }
            SkinStore.reload();
            SkinStore.setFileModelHint(result.file(), result.slim());
            refreshSkins();
            applyToSelected(result.file());
        });
    }

    private void applyToSelected(String file) {
        if (selectedPlayer == null) {
            setStatus(Text.literal("Saved " + file + ". Select a player, then click it.").formatted(Formatting.YELLOW));
            return;
        }
        String error = SkinStore.assign(selectedPlayer, file);
        setStatus(error == null
                ? Text.literal("Applied " + file + " (only you can see this).").formatted(Formatting.GREEN)
                : Text.literal(error).formatted(Formatting.RED));
    }

    /** Drag a skin PNG (e.g. downloaded from The Skindex) onto the window to add it. */
    @Override
    public void onFilesDropped(List<Path> paths) {
        List<String> added = SkinStore.importFiles(paths);
        if (added.isEmpty()) {
            setStatus(Text.literal("Only .png skin files can be added.").formatted(Formatting.YELLOW));
            return;
        }
        SkinStore.reload();
        List<String> good = new ArrayList<>();
        String lastError = null;
        for (String file : added) {
            String error = SkinStore.validate(file);
            if (error == null) {
                good.add(file);
            } else {
                SkinStore.deleteFile(file);
                lastError = error;
            }
        }
        refreshSkins();
        if (good.isEmpty()) {
            setStatus(Text.literal("Not a valid skin: " + lastError).formatted(Formatting.RED));
            return;
        }
        if (selectedPlayer != null) {
            applyToSelected(good.get(good.size() - 1));
        } else {
            setStatus(Text.literal("Added " + good.size() + " skin(s).").formatted(Formatting.GREEN));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (++refreshTimer >= 40) {
            refreshTimer = 0;
            refreshPlayers();
        }
    }

    // ---------- rendering ----------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        Ui.header(context, this.textRenderer, "IRIS'S Overrides and Tweaks", 0, 0, this.width);
        String sub = "Client-side only: nobody else sees these";
        Ui.text(context, this.textRenderer, sub, this.width - 10 - this.textRenderer.getWidth(sub) - 0, 4, Ui.MUTED);
        Ui.text(context, this.textRenderer, "Players", lx, 24, Ui.MUTED);
        Ui.text(context, this.textRenderer, "Preview", cx, 24, Ui.MUTED);

        Ui.panel(context, lx, TOP, lw, bottom - TOP);
        Ui.panel(context, cx, TOP, cw, bottom - TOP);
        Ui.panel(context, rx, TOP, rw, bottom - TOP);

        drawPlayerList(context, mouseX, mouseY);
        String hoveredSkin = drawSkinList(context, mouseX, mouseY);
        drawCenter(context, mouseX, mouseY, hoveredSkin);

        Text shownStatus = status.getString().isEmpty()
                ? Text.literal("Tip: drag a .png skin onto this window to add it.").formatted(Formatting.GRAY)
                : status;
        context.drawTextWithShadow(this.textRenderer, shownStatus, 10, this.height - 66, 0xFFFFFFFF);

        drawButtons(context, mouseX, mouseY);
        drawSlotTooltip(context, mouseX, mouseY);
    }

    private void drawPlayerList(DrawContext context, int mouseX, int mouseY) {
        context.enableScissor(lx + 1, TOP + 1, lx + lw - 1, bottom - 1);
        for (int i = 0; i < players.size(); i++) {
            int y = TOP + 3 + i * ROW_H - (int) playerScroll;
            if (y + ROW_H < TOP || y > bottom) {
                continue;
            }
            PlayerRow row = players.get(i);
            boolean selected = row.id().equals(selectedPlayer);
            boolean hovered = Ui.inside(mouseX, mouseY, lx, y, lw, ROW_H) && mouseY >= TOP && mouseY < bottom;
            if (selected) {
                Ui.roundRect(context, lx + 3, y, lw - 6, ROW_H, Ui.withAlpha(Ui.ACCENT, 0x80));
            } else if (hovered) {
                Ui.roundRect(context, lx + 3, y, lw - 6, ROW_H, 0x40FFFFFF);
            }
            String name = isSelf(row.id()) ? row.name() + " (you)" : row.name();
            Ui.text(context, this.textRenderer, name, lx + 8, y + 3, Ui.TEXT);

            int tagX = lx + lw - 10;
            if (SkinStore.assignedFile(row.id()) != null) {
                context.fill(tagX - 5, y + 5, tagX, y + 10, Ui.GOOD);
                tagX -= 10;
            }
            if (AfkTracker.isAfk(row.id())) {
                tagX -= this.textRenderer.getWidth("AFK");
                Ui.text(context, this.textRenderer, "AFK", tagX, y + 3, Ui.WARN);
            }
        }
        context.disableScissor();
    }

    /** Draws the skin list and returns the file under the mouse (or null). */
    private String drawSkinList(DrawContext context, int mouseX, int mouseY) {
        String hoveredFile = null;
        String active = selectedPlayer == null ? null : SkinStore.assignedFile(selectedPlayer);
        String last = selectedPlayer == null ? null : SkinStore.lastFile(selectedPlayer);

        context.enableScissor(rx + 1, TOP + 1, rx + rw - 1, bottom - 1);
        for (int i = 0; i < shownSkins.size(); i++) {
            int y = TOP + 3 + i * ROW_H - (int) skinScroll;
            if (y + ROW_H < TOP || y > bottom) {
                continue;
            }
            String file = shownSkins.get(i);
            boolean isActive = file.equals(active);
            boolean hovered = Ui.inside(mouseX, mouseY, rx, y, rw, ROW_H) && mouseY >= TOP && mouseY < bottom;
            if (hovered) {
                hoveredFile = file;
            }
            if (isActive) {
                Ui.roundRect(context, rx + 3, y, rw - 6, ROW_H, 0x7056D364);
            } else if (hovered) {
                Ui.roundRect(context, rx + 3, y, rw - 6, ROW_H, 0x40FFFFFF);
            }
            Ui.text(context, this.textRenderer, stripPng(file), rx + 8, y + 3, isActive ? Ui.GOOD : Ui.TEXT);
            if (!isActive && file.equals(last)) {
                Ui.text(context, this.textRenderer, "last",
                        rx + rw - this.textRenderer.getWidth("last") - 8, y + 3, Ui.MUTED);
            }
        }
        if (shownSkins.isEmpty()) {
            Ui.text(context, this.textRenderer, allSkins.isEmpty() ? "No skins yet." : "No matches.",
                    rx + 8, TOP + 8, Ui.MUTED);
            if (allSkins.isEmpty()) {
                Ui.text(context, this.textRenderer, "Drop a .png here or", rx + 8, TOP + 22, Ui.MUTED);
                Ui.text(context, this.textRenderer, "download by username.", rx + 8, TOP + 34, Ui.MUTED);
            }
        }
        context.disableScissor();
        return hoveredFile;
    }

    private int slotsY() {
        return bottom - 92;
    }

    private void drawCenter(DrawContext context, int mouseX, int mouseY, String hoveredSkin) {
        PlayerRow row = selectedRow();
        int slotsY = slotsY();
        int modelTop = TOP + 4;
        int modelBottom = slotsY - 4;

        if (row == null) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Select a player"),
                    cx + cw / 2, (modelTop + modelBottom) / 2, Ui.MUTED);
        } else {
            // soft spotlight behind the model
            context.fillGradient(cx + 1, modelTop + 12, cx + cw - 1, modelBottom, 0x0035D0BA, 0x2035D0BA);

            SkinTextures real = realSkin(row.id());
            SkinTextures shown = hoveredSkin != null
                    ? SkinStore.preview(hoveredSkin, real)
                    : SkinStore.override(row.id(), real);
            if (shown == null) {
                shown = real;
            }
            if (shown != null && shown.body() != null) {
                PlayerEntityModel model = shown.model() == PlayerSkinType.SLIM ? slimModel : wideModel;
                float height = modelBottom - modelTop - 16;
                float scale = 0.97f * height / 2.125f;
                float spin;
                float tilt = -5.0f;
                if (Ui.inside(mouseX, mouseY, cx, TOP, cw, slotsY - TOP)) {
                    spin = 20.0f + (float) (mouseX - (cx + cw / 2.0)) * 1.1f;
                    tilt = MathHelper.clamp(tilt - (float) (mouseY - (modelTop + modelBottom) / 2.0) * 0.3f, -35.0f, 35.0f);
                } else {
                    spin = (float) ((Util.getMeasuringTimeMs() / 40.0) % 360.0);
                }
                context.addPlayerSkin(model, shown.body().texturePath(), scale, tilt, spin, -1.0625f,
                        cx + 2, modelTop + 12, cx + cw - 2, modelBottom);
            }
            String title = row.name();
            if (AfkTracker.isAfk(row.id())) {
                title += "  [AFK " + Ui.formatShort(AfkTracker.idleSeconds(row.id())) + "]";
            }
            if (hoveredSkin != null) {
                title += "  -  previewing " + stripPng(hoveredSkin);
            }
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(title), cx + cw / 2, TOP + 5, Ui.TEXT);
        }
        drawInspect(context, mouseX, mouseY, row, slotsY);
    }

    private ItemStack hoveredStack = ItemStack.EMPTY;

    /** Held + worn items of the selected player, as the server shows them to every client. */
    private void drawInspect(DrawContext context, int mouseX, int mouseY, PlayerRow row, int slotsY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity target = (row == null || mc.world == null) ? null : mc.world.getPlayerByUuid(row.id());

        int slot = 20;
        int step = 22;
        int startX = slotsStartX();
        hoveredStack = ItemStack.EMPTY;

        for (int i = 0; i < INSPECT_SLOTS.length; i++) {
            int x = startX + i * step;
            ItemStack stack = target == null ? ItemStack.EMPTY : target.getEquippedStack(INSPECT_SLOTS[i]);
            boolean sel = i == inspectSlot;
            boolean over = Ui.inside(mouseX, mouseY, x, slotsY, slot, slot);
            Ui.roundRect(context, x, slotsY, slot, slot, over ? Ui.BG_HOVER : Ui.BG_LIGHT);
            Ui.roundBorder(context, x, slotsY, slot, slot, sel ? Ui.ACCENT : Ui.BORDER);
            if (!stack.isEmpty()) {
                context.drawItem(stack, x + 2, slotsY + 2);
                context.drawStackOverlay(this.textRenderer, stack, x + 2, slotsY + 2);
                if (over) {
                    hoveredStack = stack;
                }
            }
        }

        int boxY = slotsY + slot + 4;
        int boxH = bottom - 4 - boxY;
        context.enableScissor(cx + 2, boxY, cx + cw - 2, boxY + boxH);
        int ty = boxY + 1;
        if (row == null) {
            // nothing selected
        } else if (target == null) {
            Ui.text(context, this.textRenderer, "Out of range: equipment unavailable.", cx + 6, ty, Ui.MUTED);
        } else {
            ItemStack stack = target.getEquippedStack(INSPECT_SLOTS[inspectSlot]);
            Ui.text(context, this.textRenderer, INSPECT_NAMES[inspectSlot], cx + 6, ty, Ui.ACCENT);
            ty += 10;
            if (stack.isEmpty()) {
                Ui.text(context, this.textRenderer, "Empty", cx + 6, ty, Ui.MUTED);
            } else {
                List<Text> lines = Screen.getTooltipFromItem(this.client, stack);
                int maxLines = Math.max(1, (boxY + boxH - ty) / 10);
                for (int i = 0; i < lines.size() && i < maxLines; i++) {
                    context.drawTextWithShadow(this.textRenderer, lines.get(i), cx + 6, ty, 0xFFFFFFFF);
                    ty += 10;
                }
            }
        }
        context.disableScissor();
    }

    private void drawSlotTooltip(DrawContext context, int mouseX, int mouseY) {
        if (!hoveredStack.isEmpty()) {
            context.drawItemTooltip(this.textRenderer, hoveredStack, mouseX, mouseY);
        }
    }

    private int slotsStartX() {
        return cx + (cw - (INSPECT_SLOTS.length * 22 - 2)) / 2;
    }

    // ---------- helpers ----------

    private PlayerRow selectedRow() {
        if (selectedPlayer == null) {
            return null;
        }
        for (PlayerRow p : players) {
            if (p.id().equals(selectedPlayer)) {
                return p;
            }
        }
        return null;
    }

    /** The player's real (Mojang) skin, bypassing our own override. */
    private SkinTextures realSkin(UUID id) {
        MinecraftClient mc = MinecraftClient.getInstance();
        SkinStore.setBypass(true);
        try {
            ClientPlayNetworkHandler handler = mc.getNetworkHandler();
            PlayerListEntry entry = handler == null ? null : handler.getPlayerListEntry(id);
            if (entry != null) {
                return entry.getSkinTextures();
            }
            if (mc.player != null && mc.player.getUuid().equals(id)) {
                return mc.player.getSkin();
            }
            return null;
        } finally {
            SkinStore.setBypass(false);
        }
    }

    private boolean isSelf(UUID id) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player != null && mc.player.getUuid().equals(id);
    }

    private static String stripPng(String file) {
        return file.toLowerCase(Locale.ROOT).endsWith(".png") ? file.substring(0, file.length() - 4) : file;
    }

    // ---------- input ----------

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        if (clickButtons(click)) {
            return true;
        }
        double mx = click.x();
        double my = click.y();

        int slotsY = slotsY();
        int startX = slotsStartX();
        for (int i = 0; i < INSPECT_SLOTS.length; i++) {
            if (Ui.inside(mx, my, startX + i * 22, slotsY, 20, 20)) {
                inspectSlot = i;
                clickSound();
                return true;
            }
        }

        if (my < TOP || my >= bottom) {
            return false;
        }
        if (mx >= lx && mx < lx + lw) {
            int index = (int) ((my - TOP - 3 + playerScroll) / ROW_H);
            if (index >= 0 && index < players.size()) {
                selectedPlayer = players.get(index).id();
                SkinStore.setLastPlayer(selectedPlayer);
                clickSound();
                return true;
            }
        } else if (mx >= rx && mx < rx + rw) {
            int index = (int) ((my - TOP - 3 + skinScroll) / ROW_H);
            if (index >= 0 && index < shownSkins.size()) {
                if (selectedPlayer == null) {
                    setStatus(Text.literal("Select a player on the left first.").formatted(Formatting.YELLOW));
                } else {
                    applyToSelected(shownSkins.get(index));
                }
                clickSound();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        if (mouseX < cx) {
            playerScroll = MathHelper.clamp(playerScroll - verticalAmount * ROW_H * 2, 0.0, maxScroll(players.size()));
        } else if (mouseX >= rx) {
            skinScroll = MathHelper.clamp(skinScroll - verticalAmount * ROW_H * 2, 0.0, maxScroll(shownSkins.size()));
        }
        return true;
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}
