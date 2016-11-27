/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class SkullConfig {

    @Setting(value = "use-minecraft-command", comment = "loc:config.item.skullcompat")
    private boolean useMinecraftCommand = false;

    public boolean isUseMinecraftCommand() {
        return useMinecraftCommand;
    }
}
