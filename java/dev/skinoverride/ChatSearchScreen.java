package dev.skinoverride;

import dev.skinoverride.chat.ChatExtras;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/** Search everything said in chat this session. Click a line to copy it. */
public class ChatSearchScreen extends ThemedScreen {

    private static final int ROW_H = 12;
    private static String lastQuery = "";
    private static boolean lastMentionsOnly;

    private final Screen parent;
    private TextFieldWidget field;
    private boolean mentionsOnly = lastMentionsOnly;
    private List<ChatExtras.Line> results = new ArrayList<>();
    private double scroll;
    private String copiedNote = "";
    private long copiedUntil;

    private int px, py, pw, ph, listTop, listBottom;

    public ChatSearchScreen(Screen parent) {
        super(Text.literal("Chat search"));
        this.parent = parent;
    }

    @Override
    protected void build() {
        pw = Math.min(this.width - 20, 480);
        ph = this.height - 24 - 34;
        px = (this.width - pw) / 2;
        py = 24;
        listTop = py + 44;
        listBottom = py + ph - 4;

        int mentionW = 110;
        this.field = new TextFieldWidget(this.textRenderer, px + 6, py + 22, pw - 12 - mentionW - 4, 16, Text.literal("Search"));
        this.field.setPlaceholder(Text.literal("Type to search chat...").formatted(Formatting.DARK_GRAY));
        this.field.setMaxLength(60);
        this.field.setText(lastQuery);
        this.field.setChangedListener(text -> {
            lastQuery = text;
            scroll = 0;
            refresh();
        });
        this.addDrawableChild(this.field);
        this.setInitialFocus(this.field);

        button(px + pw - 6 - mentionW, py + 22, mentionW, 16,
                () -> mentionsOnly ? "Mentions: on" : "Mentions: off", () -> true, false, () -> {
                    mentionsOnly = !mentionsOnly;
                    lastMentionsOnly = mentionsOnly;
                    scroll = 0;
                    refresh();
                });

        int y = this.height - 28;
        int bw = 110;
        button(px + (pw - bw) / 2, y, bw, 20, "Done", true, this::close);
        refresh();
    }

    private void refresh() {
        results = ChatExtras.search(this.field == null ? lastQuery : this.field.getText(), mentionsOnly);
    }

    private double maxScroll() {
        return Math.max(0, results.size() * ROW_H + 4 - (listBottom - listTop));
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        // panels first, then the text field on top of them
        Ui.panel(c, px, py, pw, ph);
        Ui.header(c, this.textRenderer, "Chat search", px + 1, py + 1, pw - 2);
        Ui.panel(c, px + 4, listTop, pw - 8, listBottom - listTop);
        super.render(c, mouseX, mouseY, delta);

        scroll = MathHelper.clamp(scroll, 0.0, maxScroll());
        c.enableScissor(px + 5, listTop + 1, px + pw - 5, listBottom - 1);
        int textW = pw - 8 - 14;
        for (int i = 0; i < results.size(); i++) {
            int y = listTop + 3 + i * ROW_H - (int) scroll;
            if (y + ROW_H < listTop || y > listBottom) {
                continue;
            }
            ChatExtras.Line line = results.get(i);
            boolean hov = Ui.inside(mouseX, mouseY, px + 4, y, pw - 8, ROW_H) && mouseY >= listTop && mouseY < listBottom;
            if (hov) {
                Ui.roundRect(c, px + 6, y, pw - 12, ROW_H, 0x40FFFFFF);
            } else if (line.mention()) {
                Ui.roundRect(c, px + 6, y, pw - 12, ROW_H, Ui.withAlpha(Ui.ACCENT, 0x40));
            }
            String stamp = "[" + line.time() + "] ";
            Ui.text(c, this.textRenderer, stamp, px + 10, y + 2, Ui.MUTED);
            int sx = px + 10 + this.textRenderer.getWidth(stamp);
            String shown = this.textRenderer.trimToWidth(line.text(), Math.max(20, textW - (sx - px - 10)));
            Ui.text(c, this.textRenderer, shown, sx, y + 2, line.mention() ? Ui.WARN : Ui.TEXT);
        }
        c.disableScissor();

        if (results.isEmpty()) {
            Ui.text(c, this.textRenderer, ChatExtras.size() == 0 ? "No chat yet this session." : "No matching lines.",
                    px + 12, listTop + 8, Ui.MUTED);
        }
        String info = results.size() + " of " + ChatExtras.size() + " lines";
        if (System.currentTimeMillis() < copiedUntil) {
            info = copiedNote;
        }
        Ui.text(c, this.textRenderer, info, px + pw - 6 - this.textRenderer.getWidth(info), py + 5, Ui.MUTED);

        drawButtons(c, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled) || clickButtons(click)) {
            return true;
        }
        if (click.button() != 0) {
            return false;
        }
        double my = click.y();
        if (Ui.inside(click.x(), my, px + 4, listTop, pw - 8, listBottom - listTop)) {
            int index = (int) ((my - listTop - 3 + scroll) / ROW_H);
            if (index >= 0 && index < results.size()) {
                this.client.keyboard.setClipboard(results.get(index).text());
                copiedNote = "Copied to clipboard";
                copiedUntil = System.currentTimeMillis() + 2000L;
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
        this.client.setScreen(this.parent);
    }
}
