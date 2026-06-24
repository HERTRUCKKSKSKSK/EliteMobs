package com.example.examplemod.client;

import com.example.examplemod.EliteMobsMod;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-side packet dispatch helpers.
 * Actual packet classes live in the network sub-package.
 */
public class ClientPacketHandler {

    /**
     * Sends a request to the server to set the active DifficultyLevel for the
     * current player (identified server-side by the connection).
     *
     * @param levelOrdinal  DifficultyLevel.ordinal() of the chosen level.
     */
    public static void sendSetDifficulty(int levelOrdinal) {
        PacketDistributor.sendToServer(
                new com.example.examplemod.network.SetDifficultyPacket(levelOrdinal));
    }

    /**
     * Opens the difficulty selection screen.
     * Called from the KeyInput event on Ctrl+P.
     *
     * @param unlockedLevels  booleans received from server sync packet
     * @param activeOrdinal   currently active level ordinal
     */
    public static void openDifficultyMenu(boolean[] unlockedLevels, int activeOrdinal) {
        Minecraft.getInstance().setScreen(
                new DifficultyMenuScreen(unlockedLevels, activeOrdinal));
    }
}
