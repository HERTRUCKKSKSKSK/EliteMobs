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
                        .executes(EliteCommand::execute)
        );
    }

    private static int execute(CommandContext<CommandSourceStack> context) {

        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(
                    Component.literal("This command can only be run by a player."));
            return 0;
        }

        boolean spawned = EliteMobManager.spawnEliteNear(player);

        if (spawned) {
            context.getSource().sendSuccess(
                    () -> Component.literal("An elite mob has appeared nearby!")
                            .withStyle(ChatFormatting.GOLD),
                    true);
            return 1;
        } else {
            context.getSource().sendFailure(
                    Component.literal("Could not find a safe location to spawn the elite. Try again."));
            return 0;
        }
    }
}