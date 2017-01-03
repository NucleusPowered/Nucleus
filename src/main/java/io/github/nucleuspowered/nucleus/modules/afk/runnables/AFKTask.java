/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.afk.runnables;

import io.github.nucleuspowered.nucleus.internal.TaskBase;
import io.github.nucleuspowered.nucleus.modules.afk.handlers.AFKHandler;
import org.spongepowered.api.scheduler.Task;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class AFKTask extends TaskBase {

    @Inject private AFKHandler handler;

    @Override
    public void accept(Task task) {
        handler.onTick();
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public TimePerRun interval() {
        return new TimePerRun(1, TimeUnit.SECONDS);
    }
}
