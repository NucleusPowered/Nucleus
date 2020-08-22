/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.afk.runnables;

import io.github.nucleuspowered.nucleus.modules.afk.services.AFKHandler;
import io.github.nucleuspowered.nucleus.scaffold.task.TaskBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import javax.inject.Inject;

@NonnullByDefault
public class AFKTask implements TaskBase {

    private final AFKHandler handler;

    @Inject
    public AFKTask(INucleusServiceCollection serviceCollection) {
        this.handler = serviceCollection.getServiceUnchecked(AFKHandler.class);
    }

    @Override
    public void accept(Task task) {
        this.handler.onTick();
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public Duration interval() {
        return Duration.of(1, ChronoUnit.SECONDS);
    }
}
