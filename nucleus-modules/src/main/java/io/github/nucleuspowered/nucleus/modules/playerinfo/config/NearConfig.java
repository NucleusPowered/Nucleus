/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.playerinfo.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
@ConfigSerializable
public class NearConfig {

    @Setting(value = "max-radius", comment = "config.playerinfo.near.maxradius")
    private int maxRadius = 200;

    public int getMaxRadius() {
        return this.maxRadius;
    }

}
