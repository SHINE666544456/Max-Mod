package dev.skinoverride.mixin;

import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets Fullbright set the gamma option directly, skipping the 0..1 range check. */
@Mixin(SimpleOption.class)
public interface SimpleOptionAccessor<T> {

    @Accessor("value")
    void skinoverride$setValue(T value);
}
