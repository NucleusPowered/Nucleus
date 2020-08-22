/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kick.config;

import io.github.nucleuspowered.nucleus.configurate.config.CommonPermissionLevelConfig;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class KickConfig {

    @Setting(value = "kick-permission-levels", comment = "config.kick.permissionlevel")
    private CommonPermissionLevelConfig levelConfig = new CommonPermissionLevelConfig();

    public CommonPermissionLevelConfig getLevelConfig() {
        return this.levelConfig;
    }
}
