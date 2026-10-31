package dev.skinoverride;

import dev.skinoverride.map.SharedWaypoints;
import dev.skinoverride.map.Waypoints;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/** Friends list, the inbox of waypoints people sent you, and sending one of your waypoints to chat or a friend. */
public class WaypointSharingScreen extends ThemedScreen {

    private static final int ROW_H = 14;
    private static final int INBOX_ROWS = 4;
    private static final int INBOX_ROW_H = 16;

    private final Screen parent;
    private final ModConfig cfg = ModConfig.get();
    private TextFieldWidget friendField;
    private String selectedFriend;
    private double scroll;
    private int wpIndex;
    /** 0 = everyone in public chat, 1.. = a friend (private message) */
    private int target;
    private String message = "";
    private long messageUntil;

    private int px, py, pw, ph, listW, listTop, listBottom, rx, rw, inboxTop;

    public WaypointSharingScreen(Screen parent) {
        super(Text.literal("Share waypoints"));
        this.parent = parent;
    }

    private void say(String text) {
        message = text;
        messageUntil = System.currentTimeMillis() + 4000L;
    }

    private Waypoints.Waypoint currentWaypoint() {
        List<Waypoints.Waypoint> list = Waypoints.list();
        if (list.isEmpty()) {
            return null;
        }
        wpIndex = MathHelper.clamp(wpIndex, 0, list.size() - 1);
        return list.get(wpIndex);
    }

    private String targetName() {
        if (target <= 0 || target > cfg.friends.size()) {
            return null;
        }
        return cfg.friends.get(target - 1);
    }

    @Override
    protected void build() {
        pw = Math.min(this.width - 20, 460);
        ph = this.height - 24 - 34;
        px = (this.width - pw) / 2;
        py = 24;
        listW = 140;
        listTop = py + 44;
        listBottom = py + ph - 4;
        rx = px + listW + 14;
        rw = px + pw - 8 - rx;
        inboxTop = py + 34;

        // friends
        this.friendField = new TextFieldWidget(this.textRenderer, px + 4, py + 22, listW - 34, 16, Text.literal("Friend name"));
        this.friendField.setPlaceholder(Text.literal("Add friend...").formatted(Formatting.DARK_GRAY));
        this.friendField.setMaxLength(16);
        this.addDrawableChild(this.friendField);
        button(px + listW - 26, py + 22, 30, 16, "Add", false, () -> {
            if (SharedWaypoints.addFriend(friendField.getText())) {
                friendField.setText("");
            } else {
                say("Enter a valid player name that is not on the list yet");
            }
        });

        // share section
        int shareY = inboxTop + INBOX_ROWS * INBOX_ROW_H + 18;
        button(rx, shareY + 12, rw, 16,
                () -> {
                    Waypoints.Waypoint w = currentWaypoint();
                    return w == null ? "No waypoints here" : "Waypoint: " + w.name;
                },
                () -> !Waypoints.list().isEmpty(), false, () -> {
                    int n = Waypoints.list().size();
                    wpIndex = n == 0 ? 0 : (wpIndex + 1) % n;
                });
        button(rx, shareY + 32, rw, 16,
                () -> "Send to: " + (targetName() == null ? "Everyone (chat)" : targetName()),
                () -> true, false, () -> {
                    int n = cfg.friends.size() + 1;
                    target = (target + 1) % n;
                });
        button(rx, shareY + 54, rw, 18, () -> "Send", () -> currentWaypoint() != null && this.client.world != null, true, () -> {
            Waypoints.Waypoint w = currentWaypoint();
            if (w != null && SharedWaypoints.send(this.client, w, targetName())) {
                say("Sent \"" + w.name + "\" to " + (targetName() == null ? "chat" : targetName()));
            }
        });
        button(rx, shareY + 76, rw, 16,
                () -> "Auto-save from friends: " + (cfg.wpAutoAccept ? "on" : "off"), () -> true, false, () -> {
                    cfg.wpAutoAccept = !cfg.wpAutoAccept;
                    cfg.save();
                });

        // remove friend
        button(px + 4, py + ph - 22, listW, 16, () -> "Remove friend", () -> selectedFriend != null, false, () -> {
            SharedWaypoints.removeFriend(selectedFriend);
            selectedFriend = null;
        });

        button(px + (pw - 110) / 2, this.height - 28, 110, 20, "Done", true, this::close);
    }

    private double maxScroll() {
        return Math.max(0, cfg.friends.size() * ROW_H + 4 - (listBottom - 22 - listTop));
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        Ui.panel(c, px, py, pw, ph);
        Ui.header(c, this.textRenderer, "Share waypoints", px + 1, py + 1, pw - 2);
        Ui.panel(c, px + 4, listTop, listW, listBottom - 22 - listTop);
        super.render(c, mouseX, mouseY, delta);

        int bottom = listBottom - 22;
        scroll = MathHelper.clamp(scroll, 0.0, maxScroll());
        c.enableScissor(px + 5, listTop + 1, px + 3 + listW, bottom - 1);
        for (int i = 0; i < cfg.friends.size(); i++) {
            int y = listTop + 3 + i * ROW_H - (int) scroll;
            if (y + ROW_H < listTop || y > bottom) {
                continue;
            }
            String name = cfg.friends.get(i);
            boolean sel = name.equals(selectedFriend);
            boolean hov = Ui.inside(mouseX, mouseY, px + 4, y, listW, ROW_H) && mouseY >= listTop && mouseY < bottom;
            if (sel) {
                Ui.roundRect(c, px + 7, y, listW - 6, ROW_H, Ui.withAlpha(Ui.ACCENT, 0x80));
            } else if (hov) {
                Ui.roundRect(c, px + 7, y, listW - 6, ROW_H, 0x40FFFFFF);
            }
            Ui.text(c, this.textRenderer, name, px + 11, y + 3, Ui.TEXT);
        }
        c.disableScissor();
        if (cfg.friends.isEmpty()) {
            Ui.text(c, this.textRenderer, "No friends yet.", px + 11, listTop + 8, Ui.MUTED);
        }

        // inbox
        Ui.text(c, this.textRenderer, "Inbox: waypoints sent to you", rx, py + 24, Ui.MUTED);
        List<SharedWaypoints.Incoming> inbox = SharedWaypoints.inbox();
        int first = Math.max(0, inbox.size() - INBOX_ROWS);
        if (inbox.isEmpty()) {
            Ui.text(c, this.textRenderer, "Nothing waiting. Players with this mod can send you waypoints.", rx, inboxTop + 4, Ui.MUTED);
        }
        for (int i = first; i < inbox.size(); i++) {
            SharedWaypoints.Incoming in = inbox.get(i);
            int y = inboxTop + (inbox.size() - 1 - i) * INBOX_ROW_H;
            Ui.card(c, rx, y, rw, INBOX_ROW_H - 2, Ui.BORDER);
            String text = in.from() + ": " + in.name() + " (" + in.x() + ", " + in.y() + ", " + in.z() + ")";
            Ui.text(c, this.textRenderer, this.textRenderer.trimToWidth(text, rw - 70), rx + 4, y + 3, Ui.TEXT);
            boolean hovA = Ui.inside(mouseX, mouseY, rx + rw - 62, y + 1, 40, 12);
            boolean hovD = Ui.inside(mouseX, mouseY, rx + rw - 20, y + 1, 16, 12);
            Ui.button(c, this.textRenderer, rx + rw - 62, y + 1, 40, 12, "Save", hovA, true, true);
            Ui.button(c, this.textRenderer, rx + rw - 20, y + 1, 16, 12, "x", hovD, true, false);
        }

        int shareY = inboxTop + INBOX_ROWS * INBOX_ROW_H + 18;
        Ui.text(c, this.textRenderer, "Send one of your waypoints", rx, shareY, Ui.MUTED);
        String note = System.currentTimeMillis() < messageUntil ? message
                : "Others see the coordinates; mod users get a waypoint.";
        Ui.text(c, this.textRenderer, this.textRenderer.trimToWidth(note, rw), rx, shareY + 98, Ui.ACCENT);

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
        double mx = click.x();
        double my = click.y();

        List<SharedWaypoints.Incoming> inbox = SharedWaypoints.inbox();
        int first = Math.max(0, inbox.size() - INBOX_ROWS);
        for (int i = first; i < inbox.size(); i++) {
            int y = inboxTop + (inbox.size() - 1 - i) * INBOX_ROW_H;
            SharedWaypoints.Incoming in = inbox.get(i);
            if (Ui.inside(mx, my, rx + rw - 62, y + 1, 40, 12)) {
                SharedWaypoints.accept(this.client, in);
                say("Saved \"" + in.name() + "\"");
                clickSound();
                return true;
            }
            if (Ui.inside(mx, my, rx + rw - 20, y + 1, 16, 12)) {
                SharedWaypoints.dismiss(in);
                clickSound();
                return true;
            }
        }

        int bottom = listBottom - 22;
        if (Ui.inside(mx, my, px + 4, listTop, listW, bottom - listTop)) {
            int index = (int) ((my - listTop - 3 + scroll) / ROW_H);
            if (index >= 0 && index < cfg.friends.size()) {
                selectedFriend = cfg.friends.get(index);
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
