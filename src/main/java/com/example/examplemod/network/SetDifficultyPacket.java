package com.example.examplemod.network;

import com.example.examplemod.DifficultyLevel;
import com.example.examplemod.EliteMobsMod;
import com.example.examplemod.PlayerProgressionData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet sent from client → server when the player selects a DifficultyLevel
 * in the GUI.
 *
 * Payload: one int (DifficultyLevel ordinal).
 */
public record SetDifficultyPacket(int levelOrdinal) implements CustomPacketPayload {

    public static final Type<SetDifficultyPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EliteMobsMod.MODID, "set_difficulty"));

    public static final StreamCodec<FriendlyByteBuf, SetDifficultyPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, pkt) -> buf.writeVarInt(pkt.levelOrdinal()),
                    buf -> new SetDifficultyPacket(buf.readVarInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Server-side handler. */
    public static void handle(SetDifficultyPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            DifficultyLevel[] levels = DifficultyLevel.values();
            int ord = packet.levelOrdinal();
            if (ord < 0 || ord >= levels.length) return;

            DifficultyLevel chosen = levels[ord];
            PlayerProgressionData data = PlayerProgressionData.get(player);

            // Validate the player actually has this unlocked
            if (!data.hasUnlocked(chosen)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cYou have not unlocked " + chosen.getDisplayName() + " yet!"));
                return;
            }

            data.setActiveLevel(chosen);
            data.saveToPlayer(player);

            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§7Difficulty set to " + chosen.getColor() + "§l" + chosen.getDisplayName()));

            // Optionally send a sync-back packet to refresh the client's screen state
            PacketHelper.sendProgressionSync(player, data);
        });
    }
}
