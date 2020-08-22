/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.info.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class InfoFileConfig {

    @Setting(value = "use-default-info-section", comment = "config.info.defaultinfo")
    private boolean useDefaultFile = false;

    @Setting(value = "default-info-section", comment = "config.info.section")
    private String defaultInfoSection = "info";

    public boolean isUseDefaultFile() {
        return this.useDefaultFile;
    }

    public String getDefaultInfoSection() {
        return this.defaultInfoSection;
    }
}
