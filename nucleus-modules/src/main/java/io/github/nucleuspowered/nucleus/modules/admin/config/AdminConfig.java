/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.admin.config;

import io.github.nucleuspowered.nucleus.configurate.config.CommonPermissionLevelConfig;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class AdminConfig {

    @Setting(value = "sudo-permission-levels", comment = "config.sudo.permissionlevel")
    private CommonPermissionLevelConfig levelConfig = new CommonPermissionLevelConfig();

    public CommonPermissionLevelConfig getLevelConfig() {
        return this.levelConfig;
    }
}
