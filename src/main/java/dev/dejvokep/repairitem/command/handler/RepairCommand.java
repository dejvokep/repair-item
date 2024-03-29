/*
 * Copyright 2024 https://dejvokep.dev/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dejvokep.repairitem.command.handler;

import cloud.commandframework.context.CommandContext;
import dev.dejvokep.repairitem.RepairItem;
import dev.dejvokep.repairitem.command.function.CommandFunction;
import dev.dejvokep.repairitem.command.function.FunctionHandler;
import dev.dejvokep.repairitem.command.wrapper.Sender;
import dev.dejvokep.repairitem.command.wrapper.Target;
import dev.dejvokep.repairitem.repair.RepairResult;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Repair functions command handler.
 */
public class RepairCommand implements FunctionHandler {

    private final RepairItem plugin;
    private final CommandFunction function;

    /**
     * Initializes the command handler.
     *
     * @param plugin   the plugin instance
     * @param function the function this handler represents
     */
    public RepairCommand(@NotNull RepairItem plugin, @NotNull CommandFunction function) {
        this.plugin = plugin;
        this.function = function;
    }

    @Override
    public void accept(@NotNull CommandContext<CommandSender> context) {
        Sender sender = Sender.of(context.getSender());
        String targetName = context.getOrDefault("target", null);

        // Issuing for the sender
        if (targetName == null) {
            // Cannot be issued by a console
            if (!(context.getSender() instanceof Player)) {
                plugin.getMessenger().send(context, "repair.sender.error.players-only");
                return;
            }

            run(function, sender, Target.of((Player) context.getSender()));
            return;
        }

        // Issuing for all players
        if (plugin.getCommandRegistrar().getAllTarget().contains(targetName)) {
            if (Bukkit.getOnlinePlayers().isEmpty()) {
                plugin.getMessenger().send(context, "repair.sender.error.player-offline", message -> message.replace("{target}", plugin.getConfiguration().getString(Target.ROUTE_REPLACEMENT_ALL)));
                return;
            }

            run(function, sender, Target.online());
        }

        // Issuing for one player
        Player player = Bukkit.getPlayerExact(targetName);
        if (player == null) {
            plugin.getMessenger().send(context, "repair.sender.error.player-offline", message -> message.replace("{target}", targetName));
            return;
        }

        run(function, sender, Target.of(player));
    }

    /**
     * Runs the repair function invoked by the sender for the given target. Sends the sender and target the
     * corresponding messages.
     *
     * @param function the function to run
     * @param sender   the sender
     * @param target   the target
     */
    private void run(@NotNull CommandFunction function, @NotNull Sender sender, @NotNull Target target) {
        String targetReplacement = target.getReplacement(sender.get(), plugin.getConfiguration());
        String senderReplacement = sender.getReplacement(plugin.getConfiguration());

        // For one player
        if (target.getPlayers().size() == 1) {
            Player player = target.getOne();
            RepairResult result = plugin.getRepairer().repair(player, function);

            plugin.getMessenger().send(sender.get(), "repair.sender." + result.getStatus().getPath(function), message -> message
                    .replace("{target}", targetReplacement)
                    .replace("{repaired}", String.valueOf(result.getRepaired())));

            // Do not send both the messages if the target and sender is the same
            if (player == sender.get())
                return;

            plugin.getMessenger().send(player, "repair.target." + result.getStatus().getPath(function), message -> message
                    .replace("{sender}", senderReplacement)
                    .replace("{repaired}", String.valueOf(result.getRepaired())));
            return;
        }

        RepairResult globalResult = RepairResult.empty();
        for (Player player : target.getPlayers()) {
            RepairResult localResult = plugin.getRepairer().repair(player, function);
            globalResult = globalResult.merge(localResult);

            plugin.getMessenger().send(player, "repair.target." + localResult.getStatus().getPath(function), message -> message
                    .replace("{sender}", senderReplacement)
                    .replace("{repaired}", String.valueOf(localResult.getRepaired())));
        }

        final int repaired = globalResult.getRepaired();
        plugin.getMessenger().send(sender.get(), "repair.sender." + globalResult.getStatus().getPath(function), message -> message
                .replace("{target}", targetReplacement)
                .replace("{repaired}", String.valueOf(repaired)));
    }
}