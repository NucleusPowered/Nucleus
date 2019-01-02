/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.runnables;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.interfaces.TaskBase;
import io.github.nucleuspowered.nucleus.modules.core.CoreModule;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfig;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Core tasks. No module, must always run.
 */
@NonnullByDefault
public class CoreTask implements TaskBase, Reloadable {

    private boolean printSave = false;

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public Duration interval() {
        return Duration.of(5, ChronoUnit.MINUTES);
    }

    @Override
    public void accept(Task task) {
        Nucleus plugin = Nucleus.getNucleus();
        plugin.getStorageManager().getUserService().clearCache();

        if (this.printSave || Nucleus.getNucleus().isDebugMode()) {
            plugin.getLogger().info(plugin.getMessageProvider().getMessageWithFormat("core.savetask.starting"));
        }

        plugin.saveData();

        if (this.printSave || Nucleus.getNucleus().isDebugMode()) {
            plugin.getLogger().info(plugin.getMessageProvider().getMessageWithFormat("core.savetask.complete"));
        }
    }

    @Override
    public void onReload() {
        this.printSave = Nucleus.getNucleus().getConfigValue(CoreModule.ID, CoreConfigAdapter.class, CoreConfig::isPrintOnAutosave)
                .orElse(false);
    }
}
