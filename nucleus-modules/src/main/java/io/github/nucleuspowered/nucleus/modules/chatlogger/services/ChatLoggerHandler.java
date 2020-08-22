/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.chatlogger.services;

import io.github.nucleuspowered.nucleus.logging.AbstractLoggingHandler;
import io.github.nucleuspowered.nucleus.modules.chatlogger.config.ChatLoggingConfig;
import io.github.nucleuspowered.nucleus.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;

import java.io.IOException;

import javax.inject.Inject;

public class ChatLoggerHandler extends AbstractLoggingHandler implements ServiceBase {

    private boolean enabled = false;

    @Inject
    public ChatLoggerHandler(INucleusServiceCollection serviceCollection) {
        super("chat", "chat", serviceCollection.messageProvider(), serviceCollection.logger());
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        ChatLoggingConfig clca = serviceCollection.moduleDataProvider().getModuleConfig(ChatLoggingConfig.class);
        this.enabled = clca.isEnableLog();
        try {
            if (this.enabled && this.logger == null) {
                this.createLogger();
            } else if (!this.enabled && this.logger != null) {
                this.onShutdown();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected boolean enabledLog() {
        return this.enabled;
    }
}
