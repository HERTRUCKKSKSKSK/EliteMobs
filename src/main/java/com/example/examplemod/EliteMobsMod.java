package com.example.examplemod;

import com.example.examplemod.client.KeyInputHandler;
import com.example.examplemod.network.ProgressionSyncPacket;
import com.example.examplemod.network.RequestProgressionSyncPacket;
import com.example.examplemod.network.SetDifficultyPacket;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

@Mod(EliteMobsMod.MODID)
public class EliteMobsMod {

    public static final String MODID = "elitemobs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EliteMobsMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("╔══════════════════════════════════╗");
        LOGGER.info("║     Elite Mobs — Dante's Mod     ║");
        LOGGER.info("╚══════════════════════════════════╝");

        // Register network packets on the mod event bus
        modEventBus.addListener(this::registerPackets);

        // Register keybinds (client-only)
        modEventBus.addListener(KeyInputHandler::registerKeys);

        // Game event listeners on the NeoForge bus
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new NightEliteSpawner());
        NeoForge.EVENT_BUS.register(new EliteDeathHandler());
        NeoForge.EVENT_BUS.register(new PlayerArmorTracker());
    }

    /** Register all custom network packets (NeoForge 1.21.x style). */
    private void registerPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID).versioned("1");

        // Client → Server
        registrar.playToServer(
                SetDifficultyPacket.TYPE,
                SetDifficultyPacket.STREAM_CODEC,
                SetDifficultyPacket::handle);

        registrar.playToServer(
                RequestProgressionSyncPacket.TYPE,
                RequestProgressionSyncPacket.STREAM_CODEC,
                RequestProgressionSyncPacket::handle);

        // Server → Client
        registrar.playToClient(
                ProgressionSyncPacket.TYPE,
                ProgressionSyncPacket.STREAM_CODEC,
                ProgressionSyncPacket::handle);
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        EliteCommand.register(event.getDispatcher());
    }
}
