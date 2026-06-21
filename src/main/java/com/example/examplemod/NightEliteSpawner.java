package com.example.examplemod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.List;
import java.util.Random;

public class NightEliteSpawner {

    private static final Random RANDOM = new Random();

    // Cada cuántos ticks se evalúa el spawn (200 ticks = 10s a 20 TPS).
    private static final int CHECK_INTERVAL_TICKS = 200;

    // Probabilidad de spawn en cada chequeo (5%).
    private static final int SPAWN_CHANCE_PERCENT = 5;

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        // Solo en el Overworld.
        if (!level.dimension().equals(ServerLevel.OVERWORLD)) {
            return;
        }

        // Solo cada CHECK_INTERVAL_TICKS ticks.
        if (level.getGameTime() % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        // Solo de noche.
        if (!level.isNight()) {
            return;
        }

        // Tirada de probabilidad.
        if (RANDOM.nextInt(100) >= SPAWN_CHANCE_PERCENT) {
            return;
        }

        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }

        ServerPlayer player = players.get(RANDOM.nextInt(players.size()));

        boolean spawned = EliteMobManager.spawnEliteNear(player);

        if (spawned) {
            EliteMobsMod.LOGGER.info("Elite nocturno generado automáticamente cerca de {}",
                    player.getName().getString());
        }
    }
}