package com.example.examplemod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Listens for living entity deaths to grant loot from TOUGH and ELITE tier elites
 * (which do NOT have boss bars and therefore are not caught in the tickBossBars loop).
 *
 * CHAMPION, BERSERKER, and INFERNAL deaths are handled in EliteMobManager.tickBossBars().
 */
public class EliteDeathHandler {

    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.level().isClientSide()) return;
        if (entity.getUUID() == null) return;

        // Only handle tracked elites
        if (!EliteMobManager.isTrackedElite(entity.getUUID())) return;

        // Find the killer (must be a player)
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;

        EliteMobManager.handleEliteDeath(entity, killer);
    }
}
