/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.runnables;

import io.github.nucleuspowered.nucleus.modules.jail.services.JailHandler;
import io.github.nucleuspowered.nucleus.scaffold.task.TaskBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import javax.inject.Inject;

public class JailTask implements TaskBase {

    private final JailHandler jailHandler;
    private final PluginContainer pluginContainer;


    @Inject
    public JailTask(INucleusServiceCollection serviceCollection) {
        this.jailHandler = serviceCollection.getServiceUnchecked(JailHandler.class);
        this.pluginContainer = serviceCollection.pluginContainer();
    }

    @Override
    public void accept(Task task) {
        Sponge.getServer()
                .getOnlinePlayers()
                .stream()
                .filter(x -> this.jailHandler.isPlayerJailedCached(x))
                .filter(x -> this.jailHandler.getPlayerJailDataInternal(x).map(y -> y.expired()).orElse(false))
                .forEach(x -> this.jailHandler.unjailPlayer(x, Cause.of(EventContext.empty(), this.pluginContainer)));
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    @NonNull
    public Duration interval() {
        return Duration.of(1, ChronoUnit.SECONDS);
    }

}
