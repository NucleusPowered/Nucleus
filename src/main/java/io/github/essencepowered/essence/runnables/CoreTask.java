/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.runnables;

import com.google.inject.Inject;
import io.github.essencepowered.essence.Essence;
import io.github.essencepowered.essence.internal.TaskBase;
import io.github.essencepowered.essence.internal.services.datastore.UserConfigLoader;
import io.github.essencepowered.essence.internal.services.datastore.WorldConfigLoader;
import org.spongepowered.api.scheduler.Task;

/**
 * Core tasks. No module, must always run.
 */
public class CoreTask extends TaskBase {
    @Inject private Essence plugin;

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public int secondsPerRun() {
        return 30;
    }

    @Override
    public void accept(Task task) {
        UserConfigLoader ucl = plugin.getUserLoader();
        ucl.purgeNotOnline();
        ucl.saveAll();

        WorldConfigLoader wcl = plugin.getWorldLoader();
        wcl.saveAll();
    }
}
