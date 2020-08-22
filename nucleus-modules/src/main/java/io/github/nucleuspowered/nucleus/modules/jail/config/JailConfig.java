/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.config;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.configurate.config.CommonPermissionLevelConfig;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.List;

@ConfigSerializable
public class JailConfig {

    @Setting(value = "allowed-commands", comment = "config.jail.commands")
    private List<String> allowedCommands = Lists.newArrayList("m", "msg", "r", "mail", "rules", "info");

    @Setting(value = "mute-when-jailed",comment = "config.jail.muteWhenJailed")
    private boolean muteOnJail = false;

    @Setting(value = "jail-time-counts-online-only",comment = "config.jail.countonlineonly")
    private boolean jailOnlineOnly = false;

    @Setting(value = "require-separate-unjail-permission", comment = "config.jail.unjail")
    private boolean requireUnjailPermission = false;

    @Setting(value = "prevent-teleport-when-jailed-aggressively", comment = "config.jail.aggressive-teleport")
    private boolean aggressiveDisableTeleport = true;

    @Setting(value = "jail-permission-levels", comment = "config.jail.permissionlevel")
    private CommonPermissionLevelConfig commonPermissionLevelConfig = new CommonPermissionLevelConfig();

    public List<String> getAllowedCommands() {
        return this.allowedCommands;
    }

    public boolean isMuteOnJail() {
        return this.muteOnJail;
    }

    public boolean isJailOnlineOnly() {
        return this.jailOnlineOnly;
    }

    public boolean isRequireUnjailPermission() {
        return this.requireUnjailPermission;
    }

    public boolean aggressivelyDisableTeleportsForJailed() {
        return this.aggressiveDisableTeleport;
    }

    public CommonPermissionLevelConfig getCommonPermissionLevelConfig() {
        return this.commonPermissionLevelConfig;
    }
}
