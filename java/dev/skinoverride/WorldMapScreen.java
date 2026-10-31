package dev.skinoverride;

import dev.skinoverride.chat.ChatColors;
import dev.skinoverride.map.MapData;
import dev.skinoverride.map.Waypoints;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;

import java.util.List;

/**
 * The full map: drag to pan, scroll to zoom, right-click to drop a waypoint, click a waypoint to select it
 * (rename, recolor or delete it from the bottom bar). Shows everything your map has remembered.
 */
public class WorldMapScreen extends ThemedScreen {

    private static final int TEXV = 512;
    private static final int LIST_W = 128;
    private static final int ROW_H = 13;

    private final Screen parent;
    private double centerX;
    private double centerZ;
    private double zoom = 2.0;

    private NativeImageBackedTexture view;
    private Identifier viewId;
    private int builtVersion = -1;
    private double builtCx;
    private double builtCz;
    private double builtZoom = -1;
    private int originX;
    private int originZ;
    private int step = 1;
    private int frames;

    private int ax, ay, aw, ah;
    private boolean panning;
    private Waypoints.Waypoint selected;
    private double listScroll;
    private TextFieldWidget nameField;
    private boolean updatingField;

    public WorldMapScreen(Screen parent) {
        super(Text.literal("Map"));
        this.parent = parent;
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.player != null) {
            centerX = mc.player.getX();
            centerZ = mc.player.getZ();
        }
    }

    @Override
    protected void build() {
        ax = LIST_W + 12;
        ay = 22;
        aw = this.width - ax - 8;
        ah = this.height - ay - 34;

        if (view == null) {
            viewId = Identifier.of(SkinOverrideClient.MOD_ID, "worldmap_view");
            view = new NativeImageBackedTexture(() -> "iris world map", new NativeImage(TEXV, TEXV, true));
            this.client.getTextureManager().registerTexture(viewId, view);
            builtVersion = -1;
        }

        int y = this.height - 28;
        int g = 4;
        int nameW = 110;
        this.nameField = new TextFieldWidget(this.textRenderer, 8, y, nameW, 20, Text.literal("Waypoint name"));
        this.nameField.setPlaceholder(Text.literal("Select a waypoint").formatted(Formatting.DARK_GRAY));
        this.nameField.setMaxLength(24);
        this.nameField.setChangedListener(text -> {
            if (!updatingField && selected != null) {
                selected.name = text;
                Waypoints.save();
            }
        });
        this.addDrawableChild(this.nameField);
        syncField();

        int bw = Math.max(40, (this.width - 8 - (8 + nameW + g) - g * 5 - 8) / 6);
        int x = 8 + nameW + g;
        button(x, y, bw, 20, () -> "Delete", () -> selected != null, false, () -> {
            Waypoints.remove(selected);
            selected = null;
            syncField();
        });
        button(x + (bw + g), y, bw, 20, () -> "Color", () -> selected != null, false, () -> {
            ChatColors.Swatch[] pal = ChatColors.PALETTE;
            int next = 0;
            for (int i = 0; i < pal.length; i++) {
                if (pal[i].rgb() == selected.color) {
                    next = (i + 1) % pal.length;
                    break;
                }
            }
            selected.color = pal[next].rgb();
            Waypoints.save();
        });
        button(x + (bw + g) * 2, y, bw, 20, "Center on me", false, () -> {
            if (this.client.player != null) {
                centerX = this.client.player.getX();
                centerZ = this.client.player.getZ();
            }
        });
        button(x + (bw + g) * 3, y, bw, 20, "Add here", false, () -> {
            if (this.client.player != null) {
                int n = Waypoints.list().size() + 1;
                selected = Waypoints.add("Waypoint " + n, this.client.player.getBlockX(),
                        this.client.player.getBlockY(), this.client.player.getBlockZ());
                syncField();
            }
        });
        button(x + (bw + g) * 4, y, bw, 20, "Share", false,
                () -> this.client.setScreen(new WaypointSharingScreen(this)));
        button(x + (bw + g) * 5, y, bw, 20, "Done", true, this::close);
    }

    private void syncField() {
        if (nameField == null) {
            return;
        }
        updatingField = true;
        nameField.setText(selected == null ? "" : selected.name);
        nameField.active = selected != null;
        updatingField = false;
    }

    @Override
    public void removed() {
        if (view != null) {
            this.client.getTextureManager().destroyTexture(viewId);
            view = null;
        }
        super.removed();
    }

    /** Dim only; the panels are drawn in render(). */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xC0000000);
    }

    // ---------- coordinate helpers ----------

    private double toScreenX(double bx) {
        return ax + aw / 2.0 + (bx - centerX) * zoom;
    }

    private double toScreenY(double bz) {
        return ay + ah / 2.0 + (bz - centerZ) * zoom;
    }

    private double toBlockX(double sx) {
        return centerX + (sx - (ax + aw / 2.0)) / zoom;
    }

    private double toBlockZ(double sy) {
        return centerZ + (sy - (ay + ah / 2.0)) / zoom;
    }

    // ---------- the map texture ----------

    private void rebuildIfNeeded() {
        boolean zoomChanged = zoom != builtZoom;
        double margin = TEXV * step / 4.0;
        boolean moved = Math.abs(centerX - builtCx) > margin || Math.abs(centerZ - builtCz) > margin;
        boolean cacheChanged = MapData.version() != builtVersion && frames % 10 == 0;
        if (!zoomChanged && !moved && !cacheChanged && builtVersion >= 0) {
            return;
        }
        step = 1;
        while (aw / zoom / step > TEXV || ah / zoom / step > TEXV) {
            step *= 2;
        }
        originX = (int) Math.floor(centerX) - (TEXV / 2) * step;
        originZ = (int) Math.floor(centerZ) - (TEXV / 2) * step;

        NativeImage image = view.getImage();
        if (image == null) {
            return;
        }
        long lastKey = Long.MIN_VALUE;
        int[] arr = null;
        for (int j = 0; j < TEXV; j++) {
            int bz = originZ + j * step;
            for (int i = 0; i < TEXV; i++) {
                int bx = originX + i * step;
                int ck = bx >> 4;
                int cz = bz >> 4;
                long k = ((long) ck << 32) | (cz & 0xFFFFFFFFL);
                if (k != lastKey) {
                    arr = MapData.chunk(ck, cz);
                    lastKey = k;
                }
                int color = arr == null ? 0 : arr[((bz & 15) << 4) | (bx & 15)];
                image.setColorArgb(i, j, color == 0 ? MapData.backgroundColor() : color);
            }
        }
        view.upload();
        builtVersion = MapData.version();
        builtCx = centerX;
        builtCz = centerZ;
        builtZoom = zoom;
    }

    // ---------- rendering ----------

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        frames++;
        super.render(c, mouseX, mouseY, delta);
        rebuildIfNeeded();

        Ui.header(c, this.textRenderer, "Map", 0, 0, this.width);
        String hint = "Drag: move   Scroll: zoom   Right-click: add waypoint";
        Ui.text(c, this.textRenderer, hint, this.width - 8 - this.textRenderer.getWidth(hint), 4, Ui.MUTED);

        // map area
        Ui.panel(c, ax - 1, ay - 1, aw + 2, ah + 2);
        c.enableScissor(ax, ay, ax + aw, ay + ah);
        if (view != null) {
            int quad = (int) Math.round(TEXV * step * zoom);
            int qx = (int) Math.round(toScreenX(originX));
            int qy = (int) Math.round(toScreenY(originZ));
            c.drawTexture(RenderPipelines.GUI_TEXTURED, viewId, qx, qy, 0f, 0f, quad, quad, TEXV, TEXV, TEXV, TEXV);
        }
        drawMarkers(c, mouseX, mouseY);
        c.disableScissor();

        // pointer coordinates
        if (Ui.inside(mouseX, mouseY, ax, ay, aw, ah)) {
            String pos = "X " + (int) Math.floor(toBlockX(mouseX)) + "   Z " + (int) Math.floor(toBlockZ(mouseY));
            Ui.roundRect(c, ax + 4, ay + ah - 15, this.textRenderer.getWidth(pos) + 8, 12, 0xC0000000);
            Ui.text(c, this.textRenderer, pos, ax + 8, ay + ah - 13, Ui.TEXT);
        }

        drawList(c, mouseX, mouseY);
        drawButtons(c, mouseX, mouseY);
    }

    private void drawMarkers(DrawContext c, int mouseX, int mouseY) {
        var mc = this.client;
        // other players
        if (mc.world != null && ModConfig.get().mapPlayers) {
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player) {
                    continue;
                }
                int sx = (int) Math.round(toScreenX(p.getX()));
                int sy = (int) Math.round(toScreenY(p.getZ()));
                c.fill(sx - 3, sy - 3, sx + 4, sy + 4, 0xFF000000);
                c.fill(sx - 2, sy - 2, sx + 3, sy + 3, 0xFFFFFFFF);
                String name = p.getName().getString();
                c.drawTextWithShadow(this.textRenderer, name, sx - this.textRenderer.getWidth(name) / 2, sy + 5, Ui.TEXT);
            }
        }
        // waypoints
        for (Waypoints.Waypoint wp : Waypoints.list()) {
            int sx = (int) Math.round(toScreenX(wp.x + 0.5));
            int sy = (int) Math.round(toScreenY(wp.z + 0.5));
            boolean sel = wp == selected;
            c.fill(sx - 4, sy - 4, sx + 5, sy + 5, sel ? 0xFFFFFFFF : 0xFF000000);
            c.fill(sx - 3, sy - 3, sx + 4, sy + 4, 0xFF000000 | wp.color);
            c.drawTextWithShadow(this.textRenderer, wp.name, sx - this.textRenderer.getWidth(wp.name) / 2, sy + 6,
                    0xFF000000 | wp.color);
        }
        // you
        if (mc.player != null) {
            int sx = (int) Math.round(toScreenX(mc.player.getX()));
            int sy = (int) Math.round(toScreenY(mc.player.getZ()));
            double yaw = Math.toRadians(mc.player.getYaw());
            float psi = (float) (Math.atan2(Math.cos(yaw), -Math.sin(yaw)) + Math.PI / 2);
            Matrix3x2fStack m = c.getMatrices();
            m.pushMatrix();
            m.translate((float) sx, (float) sy);
            m.rotate(psi);
            for (int i = -6; i <= 4; i++) {
                int w = (i + 6) / 2 + 1;
                c.fill(-w, i, w + 1, i + 1, 0xFF000000);
            }
            for (int i = -5; i <= 4; i++) {
                int w = (i + 5) / 2;
                c.fill(-w, i, w + 1, i + 1, 0xFFFFFFFF);
            }
            m.popMatrix();
        }
    }

    private void drawList(DrawContext c, int mouseX, int mouseY) {
        int lx = 8;
        int top = ay;
        int bottom = ay + ah;
        Ui.panel(c, lx, top - 1, LIST_W, bottom - top + 2);
        Ui.text(c, this.textRenderer, "Waypoints", lx + 6, top + 4, Ui.MUTED);
        List<Waypoints.Waypoint> list = Waypoints.list();
        int listTop = top + 16;
        listScroll = MathHelper.clamp(listScroll, 0.0, Math.max(0, list.size() * ROW_H - (bottom - listTop)));
        c.enableScissor(lx + 1, listTop, lx + LIST_W - 1, bottom - 1);
        for (int i = 0; i < list.size(); i++) {
            int y = listTop + i * ROW_H - (int) listScroll;
            if (y + ROW_H < listTop || y > bottom) {
                continue;
            }
            Waypoints.Waypoint wp = list.get(i);
            boolean hov = Ui.inside(mouseX, mouseY, lx, y, LIST_W, ROW_H);
            if (wp == selected) {
                Ui.roundRect(c, lx + 2, y, LIST_W - 4, ROW_H, Ui.withAlpha(Ui.ACCENT, 0x80));
            } else if (hov) {
                Ui.roundRect(c, lx + 2, y, LIST_W - 4, ROW_H, 0x40FFFFFF);
            }
            c.fill(lx + 6, y + 3, lx + 12, y + 9, 0xFF000000 | wp.color);
            String name = this.textRenderer.trimToWidth(wp.name, LIST_W - 26);
            Ui.text(c, this.textRenderer, name, lx + 16, y + 3, Ui.TEXT);
        }
        if (list.isEmpty()) {
            Ui.text(c, this.textRenderer, "None yet.", lx + 6, listTop + 4, Ui.MUTED);
            Ui.text(c, this.textRenderer, "Right-click the map", lx + 6, listTop + 16, Ui.MUTED);
            Ui.text(c, this.textRenderer, "to add one.", lx + 6, listTop + 28, Ui.MUTED);
        }
        c.disableScissor();
    }

    // ---------- input ----------

    private Waypoints.Waypoint waypointAt(double mx, double my) {
        Waypoints.Waypoint best = null;
        double bestDist = 8 * 8;
        for (Waypoints.Waypoint wp : Waypoints.list()) {
            double dx = toScreenX(wp.x + 0.5) - mx;
            double dy = toScreenY(wp.z + 0.5) - my;
            double d = dx * dx + dy * dy;
            if (d < bestDist) {
                bestDist = d;
                best = wp;
            }
        }
        return best;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled) || clickButtons(click)) {
            return true;
        }
        double mx = click.x();
        double my = click.y();

        // waypoint list
        int listTop = ay + 16;
        if (Ui.inside(mx, my, 8, listTop, LIST_W, ay + ah - listTop) && click.button() == 0) {
            int index = (int) ((my - listTop + listScroll) / ROW_H);
            List<Waypoints.Waypoint> list = Waypoints.list();
            if (index >= 0 && index < list.size()) {
                selected = list.get(index);
                centerX = selected.x + 0.5;
                centerZ = selected.z + 0.5;
                syncField();
                clickSound();
                return true;
            }
            return false;
        }

        if (!Ui.inside(mx, my, ax, ay, aw, ah)) {
            return false;
        }
        if (click.button() == 1) {
            int bx = (int) Math.floor(toBlockX(mx));
            int bz = (int) Math.floor(toBlockZ(my));
            int by = this.client.player == null ? 64 : this.client.player.getBlockY();
            selected = Waypoints.add("Waypoint " + (Waypoints.list().size() + 1), bx, by, bz);
            syncField();
            clickSound();
            return true;
        }
        if (click.button() == 0) {
            Waypoints.Waypoint hit = waypointAt(mx, my);
            if (hit != null) {
                selected = hit;
                syncField();
                clickSound();
                return true;
            }
            panning = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (panning) {
            centerX -= offsetX / zoom;
            centerZ -= offsetY / zoom;
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (panning) {
            panning = false;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (Ui.inside(mouseX, mouseY, ax, ay, aw, ah) && verticalAmount != 0) {
            double bx = toBlockX(mouseX);
            double bz = toBlockZ(mouseY);
            zoom = MathHelper.clamp(zoom * (verticalAmount > 0 ? 1.25 : 0.8), 0.125, 16.0);
            // keep the block under the pointer where it was
            centerX = bx - (mouseX - (ax + aw / 2.0)) / zoom;
            centerZ = bz - (mouseY - (ay + ah / 2.0)) / zoom;
            return true;
        }
        if (Ui.inside(mouseX, mouseY, 8, ay, LIST_W, ah)) {
            listScroll -= verticalAmount * ROW_H * 2;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        Waypoints.save();
        this.client.setScreen(this.parent);
    }
}
