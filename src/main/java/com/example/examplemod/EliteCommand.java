package com.example.examplemod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class EliteCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("elite")
                .requires(source -> source.hasPermission(2))

                // /elite — random tier using player's active difficulty
                .executes(ctx -> executeRandom(ctx))

                // /elite tough|elite|champion|berserker|infernal — forced tier
                .then(Commands.literal("tough")
                    .executes(ctx -> executeTier(ctx, EliteTier.TOUGH)))
                .then(Commands.literal("elite")
                    .executes(ctx -> executeTier(ctx, EliteTier.ELITE)))
                .then(Commands.literal("champion")
                    .executes(ctx -> executeTier(ctx, EliteTier.CHAMPION)))
                .then(Commands.literal("berserker")
                    .executes(ctx -> executeTier(ctx, EliteTier.BERSERKER)))
                .then(Commands.literal("infernal")
                    .executes(ctx -> executeTier(ctx, EliteTier.INFERNAL)))

                // /elite level <name> — force-set active difficulty level (OP only)
                .then(Commands.literal("level")
                    .then(Commands.literal("limbo")
                        .executes(ctx -> executeSetLevel(ctx, DifficultyLevel.LIMBO)))
                    .then(Commands.literal("lust")
                        .executes(ctx -> executeSetLevel(ctx, DifficultyLevel.LUST)))
                    .then(Commands.literal("gluttony")
                        .executes(ctx -> executeSetLevel(ctx, DifficultyLevel.GLUTTONY)))
                    .then(Commands.literal("greed")
                        .executes(ctx -> executeSetLevel(ctx, DifficultyLevel.GREED)))
                    .then(Commands.literal("anger")
                        .executes(ctx -> executeSetLevel(ctx, DifficultyLevel.ANGER)))
                    .then(Commands.literal("heresy")
                        .executes(ctx -> executeSetLevel(ctx, DifficultyLevel.HERESY)))
                    .then(Commands.literal("violence")
                        .executes(ctx -> executeSetLevel(ctx, DifficultyLevel.VIOLENCE)))
                    .then(Commands.literal("fraud")
                        .executes(ctx -> executeSetLevel(ctx, DifficultyLevel.FRAUD)))
                    .then(Commands.literal("treachery")
                        .executes(ctx -> executeSetLevel(ctx, DifficultyLevel.TREACHERY))))

                // /elite info — show current level and unlock status
                .then(Commands.literal("info")
                    .executes(ctx -> executeInfo(ctx)))
        );
    }

    // ──────────────────────────────────────────────────────────────────────────

    private static int executeRandom(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = getPlayer(ctx);
        if (player == null) return 0;

        boolean spawned = EliteMobManager.spawnEliteNear(player);
        if (spawned) {
            ctx.getSource().sendSuccess(() -> Component.literal("Random elite spawned!")
                    .withStyle(ChatFormatting.GOLD), true);
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal("Could not find a safe spawn location."));
        return 0;
    }

    private static int executeTier(CommandContext<CommandSourceStack> ctx, EliteTier tier) {
        ServerPlayer player = getPlayer(ctx);
        if (player == null) return 0;

        boolean spawned = EliteMobManager.spawnEliteNear(player, tier);
        if (spawned) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    tier.getDisplayName() + " elite spawned!").withStyle(tier.getColor()), true);
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal("Could not find a safe spawn location."));
        return 0;
    }

    private static int executeSetLevel(CommandContext<CommandSourceStack> ctx, DifficultyLevel level) {
        ServerPlayer player = getPlayer(ctx);
        if (player == null) return 0;

        PlayerProgressionData data = PlayerProgressionData.get(player);
        data.setActiveLevel(level);
        data.saveToPlayer(player);

        ctx.getSource().sendSuccess(() -> Component.literal(
                "Difficulty set to " + level.getDisplayName())
                .withStyle(level.getColor()), true);
        return 1;
    }

    private static int executeInfo(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = getPlayer(ctx);
        if (player == null) return 0;

        PlayerProgressionData data = PlayerProgressionData.get(player);
        DifficultyLevel active = data.getActiveLevel();

        player.sendSystemMessage(Component.literal(
                "§7═══════════════════════════"));
        player.sendSystemMessage(Component.literal(
                "§6Elite Mobs — Progression Info"));
        player.sendSystemMessage(Component.literal(
                "§7Active Circle: " + active.getColor() + "§l" + active.getDisplayName()));
        player.sendSystemMessage(Component.literal(
                "§7Bosses/night: §a" + active.getBossesPerNight()));
        player.sendSystemMessage(Component.literal(
                "§7Highest XP seen: §e" + data.getHighestXpSeen()));
        player.sendSystemMessage(Component.literal(
                "§7═══════════════════════════"));

        for (DifficultyLevel lvl : DifficultyLevel.values()) {
            boolean unlocked = data.hasUnlocked(lvl);
            boolean isCurrent = lvl == active;
            String mark = isCurrent ? "§l▶" : (unlocked ? "§a✔" : "§c✘");
            player.sendSystemMessage(Component.literal(
                    mark + " " + lvl.getColor() + lvl.getDisplayName()
                            + (unlocked ? " §7[unlocked]" : " §8[locked]")));
        }

        return 1;
    }

    // ──────────────────────────────────────────────────────────────────────────

    private static ServerPlayer getPlayer(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getEntity() instanceof ServerPlayer p) return p;
        ctx.getSource().sendFailure(Component.literal("Must be run by a player."));
        return null;
    }
}
