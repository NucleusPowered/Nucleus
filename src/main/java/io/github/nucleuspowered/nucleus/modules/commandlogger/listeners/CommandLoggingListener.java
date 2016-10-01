/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.commandlogger.listeners;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.internal.listeners.ListenerBase;
import io.github.nucleuspowered.nucleus.modules.commandlogger.config.CommandLoggerConfig;
import io.github.nucleuspowered.nucleus.modules.commandlogger.config.CommandLoggerConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.commandlogger.handlers.CommandLoggerHandler;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.CommandBlockSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.state.GameStoppedServerEvent;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class CommandLoggingListener extends ListenerBase {

    @Inject private CommandLoggerConfigAdapter clc;
    @Inject private CoreConfigAdapter cca;
    @Inject private CommandLoggerHandler handler;

    @Listener(order = Order.LAST)
    public void onCommand(SendCommandEvent event, @First CommandSource source) {
        // Check source.
        CommandLoggerConfig c = clc.getNodeOrDefault();
        if (source instanceof Player && !c.getLoggerTarget().isLogPlayer()) {
            return;
        } else if (source instanceof CommandBlockSource && !c.getLoggerTarget().isLogCommandBlock()) {
            return;
        } else if (source instanceof ConsoleSource && !c.getLoggerTarget().isLogConsole()) {
            return;
        } else if (!c.getLoggerTarget().isLogOther()) {
            return;
        }

        String command = event.getCommand().toLowerCase();
        Optional<? extends CommandMapping> oc = Sponge.getCommandManager().get(command, source);
        Set<String> commands;

        // If the command exists, then get all aliases.
        if (oc.isPresent()) {
            commands = oc.get().getAllAliases().stream().map(String::toLowerCase).collect(Collectors.toSet());
        } else {
            commands = Sets.newHashSet(command);
        }

        // If whitelist, and we have the command, or if not blacklist, and we do not have the command.
        if (c.isWhitelist() == c.getCommandsToFilter().stream().map(String::toLowerCase).anyMatch(commands::contains)) {
            String message = plugin.getMessageProvider().getMessageWithFormat("commandlog.message", source.getName(), event.getCommand(), event.getArguments());
            plugin.getLogger().info(message);
            handler.queueEntry(message);
        }
    }

    @Listener
    public void onShutdown(GameStoppedServerEvent event) {
        try {
            handler.onServerShutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
