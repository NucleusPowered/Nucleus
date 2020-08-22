/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jump.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class JumpConfig {

    @Setting(value = "max-jump-distance", comment = "config.jump.maxdist")
    private int maxjump = 350;

    @Setting(value = "max-thru-distance", comment = "config.thru.maxdist")
    private int maxthru = 25;

    @Setting(value = "unstuck-distances")
    private UnstuckConfig unstuckConfig = new UnstuckConfig();

    public int getMaxJump() {
        return this.maxjump;
    }

    public int getMaxThru() {
        return this.maxthru;
    }

    public int getMaxUnstuckRadius() {
        return Math.max(1, this.unstuckConfig.hr);
    }

    public int getMaxUnstuckHeight() {
        return Math.max(1, this.unstuckConfig.h);
    }

    @ConfigSerializable
    public static class UnstuckConfig {

        @Setting(value = "horizontal-radius", comment = "config.unstuck.radius")
        int hr = 1;

        @Setting(value = "height", comment = "config.unstuck.height")
        int h = 1;

    }

}
