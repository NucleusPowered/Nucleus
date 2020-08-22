/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.chatlogger.listeners;

import io.github.nucleuspowered.nucleus.api.module.mail.event.NucleusMailEvent;
import io.github.nucleuspowered.nucleus.modules.chatlogger.config.ChatLoggingConfig;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.First;

import javax.inject.Inject;

public class MailLoggingListener extends AbstractLoggerListener {

    @Inject
    MailLoggingListener(INucleusServiceCollection serviceCollection) {
        super(serviceCollection);
    }

    @Listener(order = Order.LAST)
    public void onCommand(NucleusMailEvent event, @First CommandSource source) {
        String message = this.messageProviderService.getMessageString("chatlog.mail",
            source.getName(), event.getRecipient().getName(), event.getMessage());
        this.handler.queueEntry(message);
    }

    @Override public boolean shouldEnable(INucleusServiceCollection serviceCollection) {
        ChatLoggingConfig config = getConfig(serviceCollection);
        return config.isEnableLog() && config.isLogMail();
    }
}
