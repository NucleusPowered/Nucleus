/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.misc.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class MiscConfig {

    @Setting(value = "max-speed", comment = "config.misc.speed.max")
    private int maxSpeed = 5;

    public int getMaxSpeed() {
        return this.maxSpeed;
    }
}
