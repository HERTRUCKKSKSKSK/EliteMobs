package com.example.examplemod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.List;
import java.util.Random;

/**
 * Periodically rolls a chance to spawn an elite mob near a random player,
 * but only while it is night. Also drives the periodic update of any active
 * boss bars (Champion/Berserker tier elites).
 *
 * Tuned so that, on average, about TARGET_SPAWNS_PER_NIGHT elites spawn
 * during each in-game night (default: ~4 elites per night).
 */
public class PeriodicEliteSpawner {

    private static final Random RANDOM = new Random();

    // How often we roll the dice / update boss bars.
    private static final int CHECK_INTERVAL_TICKS = 100; // every 5 seconds

    // A full night lasts ~10000 ticks (from time ~13000 to ~23000 of the
    // 24000-tick day/night cycle). We only roll while isNight() is true, so
    // the probability is derived from the number of checks that occur during
    // that window, not the full 24000-tick cycle.
    private static final int NIGHT_DURATION_TICKS = 10000;
    private static final double TARGET_SPAWNS_PER_NIGHT = 4.0;

    // Derived probability per check: (target spawns) / (checks during a night).
    private static final double SPAWN_CHANCE_PER_CHECK =
            TARGET_SPAWNS_PER_NIGHT / ((double) NIGHT_DURATION_TICKS / CHECK_INTERVAL_TICKS);

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        // Only in the Overworld.
        if (!level.dimension().equals(ServerLevel.OVERWORLD)) {
            return;
        }

        // Only run the check every CHECK_INTERVAL_TICKS ticks.
        if (level.getGameTime() % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        // Keep boss bars (Champion/Berserker health display) up to date.
        EliteMobManager.tickBossBars(level);

        // Elites only spawn at night.
        if (!level.isNight()) {
            return;
        }

        if (RANDOM.nextDouble() >= SPAWN_CHANCE_PER_CHECK) {
            return;
        }

        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }

        ServerPlayer player = players.get(RANDOM.nextInt(players.size()));

        boolean spawned = EliteMobManager.spawnEliteNear(player);

        if (spawned) {
            EliteMobsMod.LOGGER.info("Nighttime elite spawn triggered near {}",
                    player.getName().getString());
        }
    }
}