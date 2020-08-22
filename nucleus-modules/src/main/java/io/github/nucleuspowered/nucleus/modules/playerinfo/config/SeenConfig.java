/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.playerinfo.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class SeenConfig {

    @Setting(value = "require-extended-permission-for-module-info", comment = "config.playerinfo.seen.extended")
    private boolean extendedPermRequired = false;

    public boolean isExtendedPermRequired() {
        return this.extendedPermRequired;
    }
}
