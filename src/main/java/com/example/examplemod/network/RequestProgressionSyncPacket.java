package com.example.examplemod.network;

import com.example.examplemod.EliteMobsMod;
import com.example.examplemod.PlayerProgressionData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → Server: "Please send me my current progression data."
 * Server responds with a ProgressionSyncPacket.
 *
 * No payload needed (empty packet).
 */
public record RequestProgressionSyncPacket() implements CustomPacketPayload {

    public static final Type<RequestProgressionSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EliteMobsMod.MODID, "request_sync"));

    public static final StreamCodec<FriendlyByteBuf, RequestProgressionSyncPacket> STREAM_CODEC =
            StreamCodec.of((buf, pkt) -> {}, buf -> new RequestProgressionSyncPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(RequestProgressionSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            PlayerProgressionData data = PlayerProgressionData.get(player);
            PacketHelper.sendProgressionSync(player, data);
        });
    }
}
