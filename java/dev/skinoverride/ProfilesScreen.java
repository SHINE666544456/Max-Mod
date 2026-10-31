package dev.skinoverride;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/** Save, load and delete whole settings sets, and link one to the server or world you are in. */
public class ProfilesScreen extends ThemedScreen {

    private static final int ROW_H = 14;

    private final Screen parent;
    private List<String> names = new ArrayList<>();
    private String selected;
    private double scroll;
    private TextFieldWidget nameField;
    private String status = "";
    private long statusUntil;

    private int px, py, pw, ph, listW, listTop, listBottom, rx;

    public ProfilesScreen(Screen parent) {
        super(Text.literal("Profiles"));
        this.parent = parent;
    }

    private String serverKey() {
        return Profiles.currentKey(this.client);
    }

    private void say(String text) {
        status = text;
        statusUntil = System.currentTimeMillis() + 4000L;
    }

    @Override
    protected void build() {
        pw = Math.min(this.width - 20, 440);
        ph = this.height - 24 - 34;
        px = (this.width - pw) / 2;
        py = 24;
        listW = 150;
        listTop = py + 22;
        listBottom = py + ph - 4;
        rx = px + listW + 14;
        int rw = px + pw - 8 - rx;

        refresh();

        this.nameField = new TextFieldWidget(this.textRenderer, rx, py + 22, Math.max(60, rw - 74), 16, Text.literal("Profile name"));
        this.nameField.setPlaceholder(Text.literal("New profile name...").formatted(Formatting.DARK_GRAY));
        this.nameField.setMaxLength(24);
        this.addDrawableChild(this.nameField);
        button(rx + rw - 70, py + 22, 70, 16, "Save new", false, () -> {
            String name = nameField.getText().trim();
            if (!Profiles.validName(name)) {
                say("Use 1-24 letters, numbers, spaces, - or _");
                return;
            }
            if (Profiles.save(name)) {
                selected = name;
                nameField.setText("");
                refresh();
                say("Saved \"" + name + "\"");
            } else {
                say("Could not save the profile");
            }
        });

        int bw = Math.max(60, (rw - 8) / 3);
        int y1 = py + 48;
        button(rx, y1, bw, 18, () -> "Load", () -> selected != null, true, () -> {
            say(Profiles.load(selected) ? "Loaded \"" + selected + "\"" : "Could not load that profile");
        });
        button(rx + bw + 4, y1, bw, 18, () -> "Overwrite", () -> selected != null, false, () -> {
            say(Profiles.save(selected) ? "Saved current settings into \"" + selected + "\"" : "Could not save");
        });
        button(rx + (bw + 4) * 2, y1, bw, 18, () -> "Delete", () -> selected != null, false, () -> {
            String gone = selected;
            if (Profiles.delete(gone)) {
                selected = null;
                refresh();
                say("Deleted \"" + gone + "\"");
            }
        });

        int y2 = py + 98;
        int wide = rw;
        button(rx, y2, wide, 18,
                () -> "Use \"" + (selected == null ? "..." : selected) + "\" on this server",
                () -> selected != null && serverKey() != null, false, () -> {
                    Profiles.setServerProfile(serverKey(), selected);
                    say("Linked \"" + selected + "\" to this server");
                });
        button(rx, y2 + 22, wide, 18, () -> "Stop auto-switching here",
                () -> serverKey() != null && Profiles.serverProfile(serverKey()) != null, false, () -> {
                    Profiles.setServerProfile(serverKey(), null);
                    say("Unlinked this server");
                });

        int y = this.height - 28;
        int dw = 110;
        button(px + (pw - dw) / 2, y, dw, 20, "Done", true, this::close);
    }

    private void refresh() {
        names = Profiles.list();
        if (selected != null && !names.contains(selected)) {
            selected = null;
        }
    }

    private double maxScroll() {
        return Math.max(0, names.size() * ROW_H + 4 - (listBottom - listTop));
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        Ui.panel(c, px, py, pw, ph);
        Ui.header(c, this.textRenderer, "Profiles", px + 1, py + 1, pw - 2);
        Ui.panel(c, px + 4, listTop, listW, listBottom - listTop);
        super.render(c, mouseX, mouseY, delta);

        scroll = MathHelper.clamp(scroll, 0.0, maxScroll());
        c.enableScissor(px + 5, listTop + 1, px + 3 + listW, listBottom - 1);
        String linked = Profiles.serverProfile(serverKey());
        for (int i = 0; i < names.size(); i++) {
            int y = listTop + 3 + i * ROW_H - (int) scroll;
            if (y + ROW_H < listTop || y > listBottom) {
                continue;
            }
            String name = names.get(i);
            boolean sel = name.equals(selected);
            boolean hov = Ui.inside(mouseX, mouseY, px + 4, y, listW, ROW_H) && mouseY >= listTop && mouseY < listBottom;
            if (sel) {
                Ui.roundRect(c, px + 7, y, listW - 6, ROW_H, Ui.withAlpha(Ui.ACCENT, 0x80));
            } else if (hov) {
                Ui.roundRect(c, px + 7, y, listW - 6, ROW_H, 0x40FFFFFF);
            }
            Ui.text(c, this.textRenderer, this.textRenderer.trimToWidth(name, listW - 24), px + 11, y + 3, Ui.TEXT);
            if (name.equals(linked)) {
                c.fill(px + listW - 8, y + 5, px + listW - 3, y + 10, Ui.GOOD);
            }
        }
        c.disableScissor();
        if (names.isEmpty()) {
            Ui.text(c, this.textRenderer, "No profiles yet.", px + 11, listTop + 8, Ui.MUTED);
            Ui.text(c, this.textRenderer, "Type a name and press", px + 11, listTop + 22, Ui.MUTED);
            Ui.text(c, this.textRenderer, "Save new.", px + 11, listTop + 34, Ui.MUTED);
        }

        // right side info
        String key = serverKey();
        int infoY = py + 144;
        Ui.text(c, this.textRenderer, "This server / world", rx, infoY, Ui.MUTED);
        Ui.text(c, this.textRenderer, this.textRenderer.trimToWidth(Profiles.shortKey(key), px + pw - 8 - rx), rx, infoY + 11, Ui.TEXT);
        String link = key == null ? "Join a world to link a profile to it."
                : linked == null ? "No profile linked: your normal settings are used."
                : "Auto-switches to \"" + linked + "\" when you join.";
        Ui.text(c, this.textRenderer, this.textRenderer.trimToWidth(link, px + pw - 8 - rx), rx, infoY + 24, linked == null ? Ui.MUTED : Ui.GOOD);
        Ui.text(c, this.textRenderer, "Green dot = linked to this server.", rx, infoY + 40, Ui.MUTED);
        Ui.text(c, this.textRenderer, "Changes you make in a linked server are", rx, infoY + 54, Ui.MUTED);
        Ui.text(c, this.textRenderer, "saved into its profile when you leave.", rx, infoY + 65, Ui.MUTED);
        if (System.currentTimeMillis() < statusUntil) {
            Ui.text(c, this.textRenderer, this.textRenderer.trimToWidth(status, px + pw - 8 - rx), rx, py + ph - 14, Ui.ACCENT);
        }

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
        if (Ui.inside(click.x(), click.y(), px + 4, listTop, listW, listBottom - listTop)) {
            int index = (int) ((click.y() - listTop - 3 + scroll) / ROW_H);
            if (index >= 0 && index < names.size()) {
                selected = names.get(index);
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
