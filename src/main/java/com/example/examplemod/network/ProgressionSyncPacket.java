package com.example.examplemod.network;

import com.example.examplemod.DifficultyLevel;
import com.example.examplemod.EliteMobsMod;
import com.example.examplemod.PlayerProgressionData;
import com.example.examplemod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → Client: carries the player's current progression state so the
 * client can render the DifficultyMenuScreen correctly.
 *
 * Payload:
 *   int  activeOrdinal         — DifficultyLevel.ordinal() currently active
 *   int  count                 — number of booleans (= DifficultyLevel.values().length)
 *   bool unlockedLevels[count] — whether each level is unlocked
 */
public record ProgressionSyncPacket(int activeOrdinal, boolean[] unlockedLevels)
        implements CustomPacketPayload {

    public static final Type<ProgressionSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EliteMobsMod.MODID, "progression_sync"));

    public static final StreamCodec<FriendlyByteBuf, ProgressionSyncPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, pkt) -> {
                        buf.writeVarInt(pkt.activeOrdinal());
                        buf.writeVarInt(pkt.unlockedLevels().length);
                        for (boolean b : pkt.unlockedLevels()) buf.writeBoolean(b);
                    },
                    buf -> {
                        int active = buf.readVarInt();
                        int count  = buf.readVarInt();
                        boolean[] unlocked = new boolean[count];
                        for (int i = 0; i < count; i++) unlocked[i] = buf.readBoolean();
                        return new ProgressionSyncPacket(active, unlocked);
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Client-side handler — opens the GUI. */
    public static void handle(ProgressionSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Must only touch client classes on the client thread
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientPacketHandler.openDifficultyMenu(packet.unlockedLevels(), packet.activeOrdinal());
            }
        });
    }

    /** Build from server-side PlayerProgressionData. */
    public static ProgressionSyncPacket from(PlayerProgressionData data) {
        DifficultyLevel[] levels = DifficultyLevel.values();
        boolean[] unlocked = new boolean[levels.length];
        for (int i = 0; i < levels.length; i++) {
            unlocked[i] = data.hasUnlocked(levels[i]);
        }
        return new ProgressionSyncPacket(data.getActiveLevel().ordinal(), unlocked);
    }
}
