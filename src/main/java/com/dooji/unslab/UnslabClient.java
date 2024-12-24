package com.dooji.unslab;

import com.dooji.unslab.ui.UnslabConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class UnslabClient implements ClientModInitializer {
    private static KeyBinding openConfigKey;

    @Override
    public void onInitializeClient() {
        registerKeybind();
    }

    private void registerKeybind() {
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.unslab.open_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                "category.unslab.title"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openConfigKey.wasPressed() && MinecraftClient.getInstance().currentScreen == null) {
                MinecraftClient.getInstance().setScreen(new UnslabConfigScreen(null, Text.translatable("unslab.config.title").getString()));
            }
        });
    }
}