/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.runnables;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.internal.TaskBase;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import org.spongepowered.api.scheduler.Task;

import java.util.concurrent.TimeUnit;

/**
 * Core tasks. No module, must always run.
 */
public class CoreTask extends TaskBase {
    @Inject private NucleusPlugin plugin;
    @Inject private CoreConfigAdapter cca;
    @Inject private UserDataManager uda;

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public TimePerRun interval() {
        return new TimePerRun(300, TimeUnit.SECONDS);
    }

    @Override
    public void accept(Task task) {
        if (cca.getNodeOrDefault().isDebugmode()) {
            plugin.getLogger().info(plugin.getMessageProvider().getMessageWithFormat("core.savetask.starting"));
        }

        plugin.saveData();
        uda.removeOfflinePlayers();

        if (cca.getNodeOrDefault().isDebugmode()) {
            plugin.getLogger().info(plugin.getMessageProvider().getMessageWithFormat("core.savetask.complete"));
        }
    }
}
