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
public class ScheduledBroadcast {

    @Setting(value = "name", comment = "loc:config.scheduled.broadcast.name")
    private String name;

    @Setting(value = "message", comment = "loc:config.scheduled.broadcast.message")
    private String messageToBroadcast = "This is a broadcast message!";

    @Setting(value = "task-config")
    private ScheduledTaskConfig taskConfig;

    @Nullable
    public String getName() {
        return this.name;
    }

    @Nonnull
    public String getMessageToBroadcast() {
        return this.messageToBroadcast;
    }

    public ScheduledTaskConfig getScheduledTaskConfig() {
        return this.taskConfig;
    }
}
