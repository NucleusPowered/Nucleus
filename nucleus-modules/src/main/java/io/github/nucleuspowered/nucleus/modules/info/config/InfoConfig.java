/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.info.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class InfoConfig {

    @Setting(value = "motd")
    private MotdConfig motdConfig = new MotdConfig();

    @Setting("info")
    private InfoFileConfig infoFileConfig = new InfoFileConfig();

    public boolean isShowMotdOnJoin() {
        return this.motdConfig.isShowMotdOnJoin();
    }

    public String getMotdTitle() {
        return this.motdConfig.getMotdTitle();
    }

    public boolean isMotdUsePagination() {
        return this.motdConfig.isUsePagination();
    }

    public boolean isUseDefaultFile() {
        return this.infoFileConfig.isUseDefaultFile();
    }

    public String getDefaultInfoSection() {
        return this.infoFileConfig.getDefaultInfoSection();
    }

    public float getMotdDelay() {
        return Math.max(0f, this.motdConfig.getDelay());
    }
}
