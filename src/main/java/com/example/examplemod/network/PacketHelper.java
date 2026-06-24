package com.example.examplemod.network;

import com.example.examplemod.PlayerProgressionData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Utility for server-side packet sending. */
public class PacketHelper {

    /**
     * Sends the player's full progression state to that player's client
     * so the DifficultyMenuScreen can display correct unlock status.
     */
    public static void sendProgressionSync(ServerPlayer player, PlayerProgressionData data) {
        PacketDistributor.sendToPlayer(player, ProgressionSyncPacket.from(data));
    }
}
