/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.nameban.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class NameBanConfig {

    @Setting("default-reason")
    private String defaultReason = "Your Minecraft username is not appropriate for this server. Please change it before attempting to access this server.";

    public String getDefaultReason() {
        return this.defaultReason;
    }
}
