package dev.skinoverride;

import dev.skinoverride.mixin.SimpleOptionAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

/** Fullbright by raising the brightness (gamma) option past its normal maximum while enabled. */
public final class Fullbright {

    private static final double BOOST = 16.0;
    private static boolean active;
    private static double saved = 0.5;

    private Fullbright() {
    }

    public static void tick(MinecraftClient mc) {
        if (mc.options == null) {
            return;
        }
        SimpleOption<Double> gamma = mc.options.getGamma();
        if (ModConfig.get().fullbright) {
            if (!active) {
                saved = gamma.getValue();
                active = true;
            }
            if (gamma.getValue() != BOOST) {
                set(gamma, BOOST);
            }
        } else if (active) {
            set(gamma, saved);
            active = false;
        }
    }

    /** Called when the game closes so the boosted value is never written to options.txt. */
    public static void restore() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (active && mc.options != null) {
            set(mc.options.getGamma(), saved);
            active = false;
        }
    }

    @SuppressWarnings("unchecked")
    private static void set(SimpleOption<Double> option, double value) {
        ((SimpleOptionAccessor<Double>) (Object) option).skinoverride$setValue(value);
    }
}
