/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.back.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class BackConfig {

    @Setting(value = "only-same-dimension", comment = "config.back.onlySameDimension")
    private boolean onlySameDimension = false;

    @Setting(value = "on-death", comment = "config.back.ondeath")
    private boolean onDeath = true;

    @Setting(value = "on-teleport", comment = "config.back.onteleport")
    private boolean onTeleport = true;

    @Setting(value = "on-portal", comment = "config.back.onportal")
    private boolean onPortal = false;

    public boolean isOnDeath() {
        return this.onDeath;
    }

    public boolean isOnTeleport() {
        return this.onTeleport;
    }

    public boolean isOnPortal() {
        return this.onPortal;
    }

    public boolean isOnlySameDimension() {
        return onlySameDimension;
    }
}
