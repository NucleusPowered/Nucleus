/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mute.config;

import com.google.common.collect.Lists;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.List;

@ConfigSerializable
public class MuteConfig {

    @Setting(value = "blocked-commands", comment = "loc:config.mute.blocked")
    private List<String> blockedCommands = Lists.newArrayList("me", "say");

    @Setting(value = "maximum-mute-length", comment = "loc:config.mute.maxmutelength")
    private long maxMuteLength = 604800;

    public List<String> getBlockedCommands() {
        return blockedCommands;
    }

    public long getMaximumMuteLength() {
        return maxMuteLength;
    }
}
