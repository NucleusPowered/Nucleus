/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.scheduled.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public class ScheduledConfig {

    @Setting(value = "scheduled-broadcasts", comment = "loc:config.scheduled.broadcasts")
    private List<ScheduledBroadcast> broadcasts = new ArrayList<>();

    @Setting(value = "scheduled-commands", comment = "loc:config.scheduled.commands")
    private List<ScheduledCommand> commands = new ArrayList<>();

    public List<ScheduledBroadcast> getScheduledBroadcasts() {
        return this.broadcasts;
    }

    public List<ScheduledCommand> getScheduledCommands() {
        return this.commands;
    }
}
