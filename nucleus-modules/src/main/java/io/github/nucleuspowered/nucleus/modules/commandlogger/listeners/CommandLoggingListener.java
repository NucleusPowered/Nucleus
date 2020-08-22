/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.commandlogger.listeners;

import com.google.common.collect.ImmutableSet;
import io.github.nucleuspowered.nucleus.modules.commandlogger.config.CommandLoggerConfig;
import io.github.nucleuspowered.nucleus.modules.commandlogger.services.CommandLoggerHandler;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.util.CommandNameCache;
import org.slf4j.Logger;
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
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

public class CommandLoggingListener implements IReloadableService.Reloadable, ListenerBase {

    private final CommandLoggerHandler handler;
    private final IMessageProviderService messageProvider;
    private CommandLoggerConfig c;
    private Set<String> commandsToFilter = new HashSet<>();
    private Logger logger;

    @Inject
    public CommandLoggingListener(INucleusServiceCollection serviceCollection) {
        this.handler = serviceCollection.getServiceUnchecked(CommandLoggerHandler.class);
        this.c = serviceCollection.moduleDataProvider().getModuleConfig(CommandLoggerConfig.class);
        this.messageProvider = serviceCollection.messageProvider();
        this.logger = serviceCollection.logger();
    }

    @Listener(order = Order.LAST)
    public void onCommand(SendCommandEvent event, @First CommandSource source) {
        // Check source.
        boolean accept;
        if (source instanceof Player) {
            accept = this.c.getLoggerTarget().isLogPlayer();
        } else if (source instanceof CommandBlockSource) {
            accept = this.c.getLoggerTarget().isLogCommandBlock();
        } else if (source instanceof ConsoleSource) {
            accept = this.c.getLoggerTarget().isLogConsole();
        } else {
            accept = this.c.getLoggerTarget().isLogOther();
        }

        if (!accept) {
            // We're not logging this!
            return;
        }

        String command = event.getCommand().toLowerCase();
        Set<String> commands = CommandNameCache.INSTANCE.getFromCommandAndSource(command, source);
        commands.retainAll(this.commandsToFilter);

        // If whitelist, and we have the command, or if not blacklist, and we do not have the command.
        if (this.c.isWhitelist() == !commands.isEmpty()) {
            String message = this.messageProvider.getMessageString("commandlog.message", source.getName(), event.getCommand(), event.getArguments());
            this.logger.info(message);
            this.handler.queueEntry(message);
        }
    }

    @Listener
    public void onShutdown(GameStoppedServerEvent event) {
        try {
            this.handler.onServerShutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override public void onReload(INucleusServiceCollection serviceCollection) {
        this.c = serviceCollection.moduleDataProvider().getModuleConfig(CommandLoggerConfig.class);
        this.commandsToFilter = this.c.getCommandsToFilter().stream().map(String::toLowerCase).collect(ImmutableSet.toImmutableSet());
    }
}
