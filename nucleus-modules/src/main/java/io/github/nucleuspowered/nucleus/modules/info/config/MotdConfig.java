/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.info.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class MotdConfig {

    @Setting(value = "show-motd-on-join", comment = "config.motd.onjoin")
    private boolean showMotdOnJoin = true;

    @Setting(value = "motd-login-delay", comment = "config.motd.delay")
    private float delay = 0.5f;

    @Setting(value = "motd-title", comment = "config.motd.title")
    private String motdTitle = "MOTD";

    @Setting(value = "use-pagination", comment = "config.motd.pagination")
    private boolean usePagination = true;

    public boolean isShowMotdOnJoin() {
        return this.showMotdOnJoin;
    }

    public String getMotdTitle() {
        return this.motdTitle;
    }

    public boolean isUsePagination() {
        return this.usePagination;
    }

    public float getDelay() {
        return this.delay;
    }
}
