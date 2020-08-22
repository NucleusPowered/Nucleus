/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.fly.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class FlyConfig {

    @Setting(value = "save-all-flystate-on-quit", comment = "config.fly.stateonquit")
    private boolean saveOnQuit = true;

    @Setting(value = "find-safe-location-on-login", comment = "config.fly.onlogin")
    private boolean findSafeOnLogin = true;

    @Setting(value = "require-fly-permission-on-login", comment = "config.fly.permissiononlogin")
    private boolean permissionOnLogin = false;

    public boolean isSaveOnQuit() {
        return this.saveOnQuit;
    }

    public boolean isPermissionOnLogin() {
        return this.permissionOnLogin;
    }

    public boolean isFindSafeOnLogin() {
        return this.findSafeOnLogin;
    }
}
