/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.scheduled.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@ConfigSerializable
public class ScheduledCommand {

    @Setting(value = "name", comment = "loc:config.scheduled.command.name")
    private String name;

    @Setting(value = "command", comment = "loc:config.scheduled.command.command")
    private String commandToRun = "/sponge plugins";

    @Setting(value = "task-config")
    private ScheduledTaskConfig taskConfig;

    @Nullable
    public String getName() {
        return this.name;
    }

    @Nonnull
    public String getCommandToRun() {
        return this.commandToRun;
    }

    public ScheduledTaskConfig getScheduledTaskConfig() {
        return this.taskConfig;
    }
}
