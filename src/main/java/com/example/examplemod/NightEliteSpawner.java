package com.example.examplemod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Drives nighttime elite spawning for all active players.
 *
 * Each player gets exactly DifficultyLevel.bossesPerNight spawns per night,
 * spread randomly across the night window (~10000 ticks).
 *
 * Per-player spawn budget is reset each time isNight() transitions from false → true.
 */
public class NightEliteSpawner {

    private static final Random RANDOM = new Random();
    private static final int CHECK_INTERVAL = 100; // every 5 seconds

    // Night duration in ticks (~13000–23000 in a 24000-tick day)
    private static final int NIGHT_DURATION_TICKS = 10000;

    // Per-player spawn tracking
    private static final Map<UUID, NightTracker> PLAYER_TRACKERS = new HashMap<>();

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!level.dimension().equals(ServerLevel.OVERWORLD)) return;
        if (level.getGameTime() % CHECK_INTERVAL != 0) return;

        // Update boss bars
        EliteMobManager.tickBossBars(level);

        boolean isNight = level.isNight();

        for (ServerPlayer player : level.players()) {
            UUID id = player.getUUID();
            NightTracker tracker = PLAYER_TRACKERS.computeIfAbsent(id, k -> new NightTracker());

            PlayerProgressionData data = PlayerProgressionData.get(player);
            DifficultyLevel diff = data.getActiveLevel();

            // Detect night start → reset spawn budget
            if (isNight && !tracker.wasNightLastCheck) {
                tracker.spawnBudget = diff.getBossesPerNight();
                tracker.wasNightLastCheck = true;
                EliteMobsMod.LOGGER.debug("[EliteMobs] Night started for {} — budget: {}",
                        player.getName().getString(), tracker.spawnBudget);
            }

            if (!isNight) {
                tracker.wasNightLastCheck = false;
                continue;
            }

            if (tracker.spawnBudget <= 0) continue;

            // Probability per check so that on average 'bossesPerNight' bosses
            // spawn evenly across the night.
            int checksPerNight = NIGHT_DURATION_TICKS / CHECK_INTERVAL;
            double chancePerCheck = (double) diff.getBossesPerNight() / checksPerNight;

            if (RANDOM.nextDouble() < chancePerCheck) {
                boolean spawned = EliteMobManager.spawnEliteNear(player);
                if (spawned) {
                    tracker.spawnBudget--;
                    EliteMobsMod.LOGGER.info("[EliteMobs] Night boss spawned near {} [{} remaining]",
                            player.getName().getString(), tracker.spawnBudget);
                }
            }
        }

        // Clean up trackers for disconnected players
        PLAYER_TRACKERS.entrySet().removeIf(e ->
                level.getServer().getPlayerList().getPlayer(e.getKey()) == null);
    }

    private static class NightTracker {
        boolean wasNightLastCheck = false;
        int spawnBudget = 0;
    }
}
