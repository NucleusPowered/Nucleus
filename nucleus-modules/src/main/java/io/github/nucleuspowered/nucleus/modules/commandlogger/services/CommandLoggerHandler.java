/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.commandlogger.services;

import io.github.nucleuspowered.nucleus.logging.AbstractLoggingHandler;
import io.github.nucleuspowered.nucleus.modules.commandlogger.config.CommandLoggerConfig;
import io.github.nucleuspowered.nucleus.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;

import java.io.IOException;

import javax.inject.Inject;

public class CommandLoggerHandler extends AbstractLoggingHandler implements IReloadableService.Reloadable, ServiceBase {

    private CommandLoggerConfig config;

    @Inject
    public CommandLoggerHandler(INucleusServiceCollection serviceCollection) {
        super("command", "cmds", serviceCollection.messageProvider(), serviceCollection.logger());
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        this.config = serviceCollection.moduleDataProvider().getModuleConfig(CommandLoggerConfig.class);
        try {
            if (this.config.isLogToFile() && this.logger == null) {
                this.createLogger();
            } else if (!this.config.isLogToFile() && this.logger != null) {
                this.onShutdown();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected boolean enabledLog() {
        if (this.config == null) {
            return false;
        }

        return this.config.isLogToFile();
    }
}
