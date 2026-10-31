package dev.skinoverride.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** One movable HUD element. prepare() builds its content, then width()/height()/render() use it. */
public interface HudWidget {

    String id();

    String label();

    /** Default position as 0..1 fractions of the free screen space (0,0 = top-left, 1,1 = bottom-right). */
    float defaultX();

    float defaultY();

    boolean enabled();

    /** False to hide in the current situation (e.g. while an inventory is open). */
    default boolean visible(MinecraftClient mc) {
        return true;
    }

    /** Builds the content. preview=true uses demo data so the layout editor always has something to show. */
    void prepare(MinecraftClient mc, boolean preview);

    int width();

    int height();

    void render(DrawContext context, MinecraftClient mc, int x, int y, boolean preview);
}
