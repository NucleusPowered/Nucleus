/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.connectionmessages.listeners;

import io.github.nucleuspowered.nucleus.modules.connectionmessages.config.ConnectionMessagesConfig;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.type.Include;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.channel.MessageChannel;

public class ConnectionMessagesForceListener implements ListenerBase.Conditional {

    @Listener(order = Order.FIRST)
    @Include({ClientConnectionEvent.Disconnect.class, ClientConnectionEvent.Join.class})
    public void onPlayerLogin(MessageChannelEvent joinEvent) {
        joinEvent.setChannel(MessageChannel.TO_ALL);
    }

    @Override
    public boolean shouldEnable(INucleusServiceCollection serviceCollection) {
        return serviceCollection.moduleDataProvider().getModuleConfig(ConnectionMessagesConfig.class).isForceForAll();
    }
}
