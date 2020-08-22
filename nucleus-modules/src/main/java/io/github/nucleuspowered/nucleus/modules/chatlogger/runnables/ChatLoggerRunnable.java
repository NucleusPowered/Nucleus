/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.chatlogger.runnables;

import io.github.nucleuspowered.nucleus.modules.chatlogger.config.ChatLoggingConfig;
import io.github.nucleuspowered.nucleus.modules.chatlogger.services.ChatLoggerHandler;
import io.github.nucleuspowered.nucleus.scaffold.task.TaskBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.GameState;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import javax.inject.Inject;

@NonnullByDefault
public class ChatLoggerRunnable implements TaskBase, IReloadableService.Reloadable {

    private final ChatLoggerHandler handler;
    private ChatLoggingConfig config = new ChatLoggingConfig();

    @Inject
    public ChatLoggerRunnable(INucleusServiceCollection serviceCollection) {
        this.handler = serviceCollection.getServiceUnchecked(ChatLoggerHandler.class);
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public Duration interval() {
        return Duration.of(1, ChronoUnit.SECONDS);
    }


    @Override
    public void accept(Task task) {
        if (Sponge.getGame().getState() == GameState.SERVER_STOPPED) {
            return;
        }

        if (this.config.isEnableLog()) {
            this.handler.onTick();
        }
    }

    @Override public void onReload(INucleusServiceCollection serviceCollection) {
        this.config = serviceCollection.moduleDataProvider().getModuleConfig(ChatLoggingConfig.class);
    }
}
