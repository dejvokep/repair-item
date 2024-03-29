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
package dev.dejvokep.repairitem.command;

import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.BukkitCommandManager;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.CommandMeta;
import dev.dejvokep.repairitem.RepairItem;
import dev.dejvokep.repairitem.command.function.CommandFunction;
import dev.dejvokep.repairitem.command.function.FunctionHandler;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Command registrar for the <code>/repair</code> command.
 */
public class CommandRegistrar {

    /**
     * The base permission node.
     */
    public static final String PERMISSION_BASE = "repairitem";

    private final Set<String> allTarget = new HashSet<>();
    private final RepairItem plugin;

    /**
     * Registers all commands to the given plugin instance.
     *
     * @param plugin the plugin instance
     * @throws Exception thrown if failed to construct the command manager
     */
    public CommandRegistrar(@NotNull RepairItem plugin) throws Exception {
        this.plugin = plugin;

        // Create the manager
        CommandManager<CommandSender> manager = new BukkitCommandManager<>(plugin, CommandExecutionCoordinator.simpleCoordinator(), Function.identity(), Function.identity());

        for (CommandFunction function : CommandFunction.values()) {
            List<String> literals = plugin.getConfiguration().getStringList("command.function." + function.getPath());

            // No literal to assign to the function
            if (literals.isEmpty())
                continue;

            // Aliases and handler
            String[] aliases = literals.size() == 1 ? new String[0] : literals.subList(1, literals.size()).toArray(new String[literals.size() - 1]);
            FunctionHandler handler = function.initHandler(plugin);

            // Register for self and targeted repair
            manager.command(manager.commandBuilder("repair")
                    .literal(literals.get(0), aliases)
                    .permission(String.format("%s.%s.self", PERMISSION_BASE, function.getPermission()))
                    .meta(CommandMeta.DESCRIPTION, function.getDescription())
                    .handler(handler::accept).build());

            if (!function.hasTarget())
                continue;

            manager.command(manager.commandBuilder("repair")
                    .literal(literals.get(0), aliases)
                    .argument(StringArgument.of("target"))
                    .permission(String.format("%s.%s.other", PERMISSION_BASE, function.getPermission()))
                    .meta(CommandMeta.DESCRIPTION, function.getDescription())
                    .handler(handler::accept).build());
        }
    }

    /**
     * Reloads the registrar.
     */
    public void reload() {
        allTarget.clear();
        allTarget.addAll(plugin.getConfiguration().getStringList("command.target.all"));
    }

    /**
     * Returns the set of strings which can be specified as the <code>target</code> command argument to represent all
     * online players.
     *
     * @return the set of placeholders representing all online players
     */
    @NotNull
    public Set<String> getAllTarget() {
        return allTarget;
    }
}