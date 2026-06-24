package com.example.examplemod;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Checks every 40 ticks (2 seconds) whether a player has equipped armor
 * that would permanently unlock higher difficulty circles.
 *
 * The unlock is "sticky" — once you equip iron armor, you can always access
 * Gluttony/Greed/Anger/Heresy even if you later swap back to leather.
 */
public class PlayerArmorTracker {

    private static final int CHECK_INTERVAL = 40; // ticks

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Rate-limit the check
        if (player.tickCount % CHECK_INTERVAL != 0) return;

        PlayerProgressionData data = PlayerProgressionData.get(player);
        data.checkAndRecordArmor(player);
    }
}
