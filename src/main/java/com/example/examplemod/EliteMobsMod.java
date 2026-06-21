package com.example.examplemod;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(EliteMobsMod.MODID)
public class EliteMobsMod {

    // IMPORTANT: this id must exactly match the modId declared in
    // src/main/resources/META-INF/neoforge.mods.toml
    public static final String MODID = "elitemobs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EliteMobsMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Elite Mobs loaded!");

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new PeriodicEliteSpawner());
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        EliteCommand.register(event.getDispatcher());
    }
}