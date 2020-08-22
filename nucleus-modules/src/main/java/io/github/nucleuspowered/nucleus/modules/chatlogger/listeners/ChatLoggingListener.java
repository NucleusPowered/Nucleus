/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.chatlogger.listeners;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.chatlogger.config.ChatLoggingConfig;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;

import java.util.Optional;

import javax.inject.Inject;

public class ChatLoggingListener extends AbstractLoggerListener {

    @Inject
    ChatLoggingListener(INucleusServiceCollection serviceCollection) {
        super(serviceCollection);
    }

    @Listener(order = Order.LAST)
    public void onCommand(MessageChannelEvent.Chat event) {
        Util.onSourceSimulatedOr(event, this::getSource, this::onCommand);
    }

    private void onCommand(MessageChannelEvent.Chat event, CommandSource source) {
        log(event.getMessage().toPlain(), source);
    }

    @Listener(order = Order.LAST)
    public void onCommand(SendCommandEvent event) {
        if (event.getCommand().equalsIgnoreCase("say") || event.getCommand().equalsIgnoreCase("minecraft:say")) {
            Util.onSourceSimulatedOr(event, this::getSource, this::onCommand);
        }
    }

    private void onCommand(SendCommandEvent event, CommandSource source) {
        log(event.getArguments(), source);
    }

    private void log(String s, CommandSource source) {
        String message = this.messageProviderService.getMessageString("chatlog.chat", source.getName(), s);
        this.handler.queueEntry(message);
    }

    @Override public boolean shouldEnable(INucleusServiceCollection serviceCollection) {
        ChatLoggingConfig config = getConfig(serviceCollection);
        return config.isEnableLog() && config.isLogChat();
    }

    private Optional<CommandSource> getSource(Event event) {
        return event.getCause().first(CommandSource.class);
    }

}
