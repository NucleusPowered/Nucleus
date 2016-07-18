/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warn.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class WarnConfig {

    @Setting(value = "show-login", comment = "loc:config.warn.showonlogin")
    private boolean showOnLogin = true;

    public boolean isShowOnLogin() {
        return showOnLogin;
    }
}
