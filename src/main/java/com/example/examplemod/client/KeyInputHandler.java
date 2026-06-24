package com.example.examplemod.client;

import com.example.examplemod.EliteMobsMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Handles the Ctrl+P keybind to open the Circle Selection GUI.
 *
 * The keybind defaults to Ctrl+P, but can be rebound in vanilla Controls menu.
 */
@EventBusSubscriber(modid = EliteMobsMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class KeyInputHandler {

    public static final KeyMapping OPEN_MENU_KEY = new KeyMapping(
            "key.elitemobs.open_menu",         // translation key
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,                   // default: P (player holds Ctrl)
            "key.categories.elitemobs"         // category
    );

    /** Register keybind on mod event bus (must be called from EliteMobsMod constructor). */
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MENU_KEY);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (OPEN_MENU_KEY.consumeClick()) {
            // Request server to send current unlock/active state before opening GUI
            PacketDistributor.sendToServer(
                    new com.example.examplemod.network.RequestProgressionSyncPacket());
        }
    }
}
