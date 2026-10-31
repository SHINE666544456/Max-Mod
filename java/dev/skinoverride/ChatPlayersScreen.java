package dev.skinoverride;

import dev.skinoverride.chat.ChatColors;
import dev.skinoverride.chat.ChatProcessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pick a player, then pick a color for their chat name, their chat messages or their nametag
 * (solid, rainbow or cycling, or any hex color).
 */
public class ChatPlayersScreen extends ThemedScreen {

    private static final int ROW_H = 14;
    private static final String[] TARGETS = {"Chat name", "Message", "Nametag"};

    private record Entry(String key, String display) {
    }

    private final Screen parent;
    private final ModConfig cfg = ModConfig.get();
    private final List<Entry> entries = new ArrayList<>();

    private String selectedKey;
    private int target;
    private double scroll;
    private TextFieldWidget addField;
    private TextFieldWidget hexField;

    private int px, py, pw, ph, listW, listTop, listBottom, rx, tabW, pickY, hexX;

    public ChatPlayersScreen(Screen parent) {
        super(Text.literal("Per-player colors"));
        this.parent = parent;
    }

    @Override
    protected void build() {
        pw = Math.min(this.width - 20, 440);
        ph = this.height - 24 - 34;
        px = (this.width - pw) / 2;
        py = 24;
        listW = 130;
        listTop = py + 44;
        listBottom = py + ph - 4;
        rx = px + listW + 14;
        tabW = Math.max(54, Math.min(76, (px + pw - 8 - rx - 8) / 3));
        pickY = py + 44;

        this.addField = new TextFieldWidget(this.textRenderer, px + 4, py + 22, listW - 34, 16, Text.literal("Player name"));
        this.addField.setPlaceholder(Text.literal("Add player...").formatted(Formatting.DARK_GRAY));
        this.addField.setMaxLength(16);
        this.addDrawableChild(this.addField);
        button(px + listW - 26, py + 22, 30, 16, "Add", false, this::addPlayer);

        // custom color by hex code, next to the color grid
        hexX = rx + ColorPicker.width() + 8;
        int avail = px + pw - 8 - hexX;
        int hexW = Math.max(40, Math.min(72, avail - 34));
        this.hexField = new TextFieldWidget(this.textRenderer, hexX, pickY + 14, hexW, 16, Text.literal("Hex color"));
        this.hexField.setPlaceholder(Text.literal("#RRGGBB").formatted(Formatting.DARK_GRAY));
        this.hexField.setMaxLength(7);
        this.addDrawableChild(this.hexField);
        button(hexX + hexW + 4, pickY + 14, 30, 16, "Set", false, () -> applyHex(hexField.getText()));

        int y = this.height - 28;
        int bw = 110;
        int startX = px + (pw - (bw * 2 + 4)) / 2;
        button(startX, y, bw, 20, () -> "Reset player", () -> selectedKey != null && cfg.chatPlayers.containsKey(selectedKey),
                false, () -> {
                    cfg.chatPlayers.remove(selectedKey);
                    cfg.save();
                });
        button(startX + bw + 4, y, bw, 20, "Done", true, this::close);

        refreshEntries();
    }

    private void refreshEntries() {
        Map<String, String> map = new LinkedHashMap<>();
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (handler != null) {
            for (PlayerListEntry e : handler.getPlayerList()) {
                String name = e.getProfile().name();
                map.put(name.toLowerCase(Locale.ROOT), name);
            }
        }
        for (Map.Entry<String, ModConfig.PlayerChat> e : cfg.chatPlayers.entrySet()) {
            String display = e.getValue().name == null || e.getValue().name.isEmpty() ? e.getKey() : e.getValue().name;
            map.put(e.getKey(), display);
        }
        entries.clear();
        for (Map.Entry<String, String> e : map.entrySet()) {
            entries.add(new Entry(e.getKey(), e.getValue()));
        }
        entries.sort(Comparator.comparing(e -> e.display().toLowerCase(Locale.ROOT)));
    }

    private void addPlayer() {
        String name = addField.getText().trim();
        if (!name.matches("[A-Za-z0-9_]{1,16}")) {
            return;
        }
        String key = name.toLowerCase(Locale.ROOT);
        ModConfig.PlayerChat pc = cfg.chatPlayers.computeIfAbsent(key, k -> new ModConfig.PlayerChat());
        pc.name = name;
        cfg.save();
        addField.setText("");
        selectedKey = key;
        refreshEntries();
    }

    private ModConfig.PlayerChat selectedChat(boolean create) {
        if (selectedKey == null) {
            return null;
        }
        ModConfig.PlayerChat pc = cfg.chatPlayers.get(selectedKey);
        if (pc == null && create) {
            pc = new ModConfig.PlayerChat();
            pc.name = selectedDisplay();
            cfg.chatPlayers.put(selectedKey, pc);
        }
        return pc;
    }

    private String selectedDisplay() {
        for (Entry e : entries) {
            if (e.key().equals(selectedKey)) {
                return e.display();
            }
        }
        return selectedKey == null ? "" : selectedKey;
    }

    private int currentValue() {
        ModConfig.PlayerChat pc = selectedChat(false);
        if (pc == null) {
            return ChatColors.DEFAULT;
        }
        return target == 0 ? pc.nameColor : target == 1 ? pc.msgColor : pc.tagColor;
    }

    private void setValue(int value) {
        ModConfig.PlayerChat pc = selectedChat(true);
        if (target == 0) {
            pc.nameColor = value;
        } else if (target == 1) {
            pc.msgColor = value;
        } else {
            pc.tagColor = value;
        }
        if (pc.nameColor == ChatColors.DEFAULT && pc.msgColor == ChatColors.DEFAULT && pc.tagColor == ChatColors.DEFAULT) {
            cfg.chatPlayers.remove(selectedKey);
        }
        cfg.save();
        clickSound();
    }

    /** Accepts "#RRGGBB" or "RRGGBB". */
    private void applyHex(String text) {
        if (selectedKey == null) {
            return;
        }
        String hex = text.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.matches("[0-9A-Fa-f]{6}")) {
            setValue(Integer.parseInt(hex, 16));
        }
    }

    private double maxScroll() {
        return Math.max(0, entries.size() * ROW_H + 4 - (listBottom - listTop));
    }

    // ---------- rendering ----------

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);

        Ui.panel(c, px, py, pw, ph);
        Ui.header(c, this.textRenderer, "Per-player colors", px + 1, py + 1, pw - 2);

        Ui.panel(c, px + 4, listTop, listW, listBottom - listTop);
        scroll = MathHelper.clamp(scroll, 0.0, maxScroll());
        c.enableScissor(px + 5, listTop + 1, px + 3 + listW, listBottom - 1);
        for (int i = 0; i < entries.size(); i++) {
            int y = listTop + 3 + i * ROW_H - (int) scroll;
            if (y + ROW_H < listTop || y > listBottom) {
                continue;
            }
            Entry e = entries.get(i);
            boolean sel = e.key().equals(selectedKey);
            boolean hov = Ui.inside(mouseX, mouseY, px + 4, y, listW, ROW_H) && mouseY >= listTop && mouseY < listBottom;
            if (sel) {
                Ui.roundRect(c, px + 7, y, listW - 6, ROW_H, Ui.withAlpha(Ui.ACCENT, 0x80));
            } else if (hov) {
                Ui.roundRect(c, px + 7, y, listW - 6, ROW_H, 0x40FFFFFF);
            }
            Ui.text(c, this.textRenderer, e.display(), px + 11, y + 3, Ui.TEXT);
            ModConfig.PlayerChat pc = cfg.chatPlayers.get(e.key());
            if (pc != null) {
                int dot = pc.nameColor >= 0 ? pc.nameColor : pc.msgColor >= 0 ? pc.msgColor
                        : pc.tagColor >= 0 ? pc.tagColor : (Ui.ACCENT & 0xFFFFFF);
                c.fill(px + listW - 8, y + 5, px + listW - 3, y + 10, 0xFF000000 | dot);
            }
        }
        c.disableScissor();

        if (selectedKey == null) {
            Ui.text(c, this.textRenderer, "Pick a player on the left,", rx, pickY + 6, Ui.MUTED);
            Ui.text(c, this.textRenderer, "or type a name and press Add.", rx, pickY + 18, Ui.MUTED);
        } else {
            // target tabs
            for (int i = 0; i < TARGETS.length; i++) {
                int tx = rx + i * (tabW + 3);
                boolean sel = i == target;
                boolean hov = Ui.inside(mouseX, mouseY, tx, py + 22, tabW, 16);
                Ui.roundRect(c, tx, py + 22, tabW, 16, sel ? Ui.ACCENT_FILL : hov ? Ui.BG_HOVER : Ui.BG_LIGHT);
                Ui.roundBorder(c, tx, py + 22, tabW, 16, sel ? Ui.ACCENT : Ui.BORDER);
                String label = TARGETS[i];
                c.drawTextWithShadow(this.textRenderer, label, tx + (tabW - this.textRenderer.getWidth(label)) / 2,
                        py + 26, sel ? 0xFFFFFFFF : Ui.TEXT);
            }

            ColorPicker.draw(c, this.textRenderer, rx, pickY, mouseX, mouseY, currentValue());
            Ui.text(c, this.textRenderer, "Custom color", hexX, pickY + 2, Ui.MUTED);

            ModConfig.PlayerChat pc = selectedChat(false);
            int nameMode = pc != null && pc.nameColor != ChatColors.DEFAULT ? pc.nameColor : cfg.chatDefaultNameColor;
            int msgMode = pc != null && pc.msgColor != ChatColors.DEFAULT ? pc.msgColor : cfg.chatDefaultMsgColor;
            int tagMode = pc != null && pc.tagColor != ChatColors.DEFAULT ? pc.tagColor : cfg.nametagDefaultColor;

            int py2 = pickY + ColorPicker.height() + 8;
            Ui.text(c, this.textRenderer, "Preview for " + selectedDisplay(), rx, py2, Ui.MUTED);
            c.drawTextWithShadow(this.textRenderer, ChatProcessor.sample(selectedDisplay(), nameMode, msgMode),
                    rx, py2 + 12, 0xFFFFFFFF);
            Text tag = ChatColors.apply(Text.literal(selectedDisplay()), tagMode);
            int tw = this.textRenderer.getWidth(tag);
            Ui.roundRect(c, rx, py2 + 26, tw + 10, 13, 0x90000000);
            c.drawTextWithShadow(this.textRenderer, tag, rx + 5, py2 + 29, 0xFFFFFFFF);
        }

        drawButtons(c, mouseX, mouseY);
    }

    // ---------- input ----------

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled) || clickButtons(click)) {
            return true;
        }
        if (click.button() != 0) {
            return false;
        }
        double mx = click.x();
        double my = click.y();

        if (selectedKey != null) {
            for (int i = 0; i < TARGETS.length; i++) {
                if (Ui.inside(mx, my, rx + i * (tabW + 3), py + 22, tabW, 16)) {
                    target = i;
                    clickSound();
                    return true;
                }
            }
            Integer chosen = ColorPicker.hit(mx, my, rx, pickY);
            if (chosen != null) {
                setValue(chosen);
                return true;
            }
        }

        if (Ui.inside(mx, my, px + 4, listTop, listW, listBottom - listTop)) {
            int index = (int) ((my - listTop - 3 + scroll) / ROW_H);
            if (index >= 0 && index < entries.size()) {
                selectedKey = entries.get(index).key();
                clickSound();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = MathHelper.clamp(scroll - verticalAmount * ROW_H * 2, 0.0, maxScroll());
        return true;
    }

    @Override
    public void close() {
        cfg.save();
        this.client.setScreen(this.parent);
    }
}
