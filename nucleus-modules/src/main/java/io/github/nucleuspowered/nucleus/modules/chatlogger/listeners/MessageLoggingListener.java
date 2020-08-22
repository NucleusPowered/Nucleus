/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.chatlogger.listeners;

import io.github.nucleuspowered.nucleus.api.module.message.event.NucleusMessageEvent;
import io.github.nucleuspowered.nucleus.modules.chatlogger.config.ChatLoggingConfig;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;

public class MessageLoggingListener extends AbstractLoggerListener {

    MessageLoggingListener(INucleusServiceCollection serviceCollection) {
        super(serviceCollection);
    }

    @Listener(order = Order.LAST)
    public void onCommand(NucleusMessageEvent event) {
        String message = this.messageProviderService.getMessageString("chatlog.message",
            event.getSender().getName(), event.getRecipient().getName(), event.getMessage());
        this.handler.queueEntry(message);
    }

    @Override public boolean shouldEnable(INucleusServiceCollection serviceCollection) {
        ChatLoggingConfig config = getConfig(serviceCollection);
        return config.isEnableLog() && config.isLogMessages();
    }
}
