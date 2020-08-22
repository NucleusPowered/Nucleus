/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.chatlogger.listeners;

import io.github.nucleuspowered.nucleus.modules.chatlogger.config.ChatLoggingConfig;
import io.github.nucleuspowered.nucleus.modules.chatlogger.services.ChatLoggerHandler;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;

import javax.inject.Inject;

abstract class AbstractLoggerListener implements ListenerBase.Conditional {

    final ChatLoggerHandler handler;
    final IMessageProviderService messageProviderService;

    @Inject
    AbstractLoggerListener(INucleusServiceCollection serviceCollection) {
        this.handler = serviceCollection.getServiceUnchecked(ChatLoggerHandler.class);
        this.messageProviderService = serviceCollection.messageProvider();
    }

    ChatLoggingConfig getConfig(INucleusServiceCollection serviceCollection) {
        return serviceCollection.moduleDataProvider().getModuleConfig(ChatLoggingConfig.class);
    }

}
