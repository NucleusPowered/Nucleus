/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.deathmessage.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class DeathMessageConfig {

    @Setting(value = "enable-death-messages", comment = "config.deathmessages.enable")
    private boolean enableDeathMessages = true;

    @Setting(value = "force-show-all-death-messages", comment = "config.deathmessages.showall")
    private boolean forceForAll = true;

    public boolean isEnableDeathMessages() {
        return this.enableDeathMessages;
    }

    public boolean isForceForAll() {
        return this.forceForAll;
    }
}
