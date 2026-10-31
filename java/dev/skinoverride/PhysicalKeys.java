package dev.skinoverride;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Reads whether the key or mouse button behind a key binding is physically held down right now. */
public final class PhysicalKeys {

    private PhysicalKeys() {
    }

    public static boolean isDown(MinecraftClient mc, KeyBinding binding) {
        InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(binding);
        long handle = mc.getWindow().getHandle();
        if (key.getCategory() == InputUtil.Type.KEYSYM) {
            return GLFW.glfwGetKey(handle, key.getCode()) == GLFW.GLFW_PRESS;
        }
        if (key.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(handle, key.getCode()) == GLFW.GLFW_PRESS;
        }
        return false;
    }
}
