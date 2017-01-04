/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.scheduled;


import uk.co.drnaylor.quickstart.annotations.ModuleData;

import java.time.ZonedDateTime;

import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.internal.services.ScheduledManager;
import io.github.nucleuspowered.nucleus.modules.scheduled.config.ScheduledConfig;
import io.github.nucleuspowered.nucleus.modules.scheduled.config.ScheduledConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.scheduled.tasks.BroadcastTask;
import io.github.nucleuspowered.nucleus.modules.scheduled.tasks.CommandTask;

@ModuleData(id = ScheduledModule.ID, name = "Scheduler")
public class ScheduledModule extends ConfigurableModule<ScheduledConfigAdapter> {

    public static final String ID = "scheduled";

    @Override
    public ScheduledConfigAdapter createAdapter() {
        return new ScheduledConfigAdapter();
    }

    @Override
    protected void performPreTasks() throws Exception {
        super.performPreTasks();
        this.plugin.getLogger().info(String.format("Starting Scheduled Tasks, Current Time Zone is: [ %s ].",
                ZonedDateTime.now().getOffset().getId()));
    }

    @Override
    public void onEnable() {
        super.onEnable();
        ScheduledConfigAdapter sca = (ScheduledConfigAdapter) this.getConfigAdapter().get();
        this.plugin.getLogger().info("Registering Scheduled Tasks.");
        ScheduledConfig config = sca.getNodeOrDefault();
        ScheduledManager scheduledManager = this.plugin.getScheduledManager();
        config.getScheduledBroadcasts().forEach((scheduledBroadcast -> {
            scheduledManager.addScheduledTask(
                    new BroadcastTask(scheduledBroadcast.getName(),
                        scheduledBroadcast.getMessageToBroadcast(),
                        scheduledBroadcast.getScheduledTaskConfig().getDelayedTimeValue())
            );
        }));
        config.getScheduledCommands().forEach((scheduledCommand -> {
            scheduledManager.addScheduledTask(
                    new CommandTask(scheduledCommand.getName(),
                            scheduledCommand.getCommandToRun(),
                            scheduledCommand.getScheduledTaskConfig().getDelayedTimeValue())
            );
        }));
        this.plugin.getLogger().info("Registered Scheduled Tasks.");
    }
}
