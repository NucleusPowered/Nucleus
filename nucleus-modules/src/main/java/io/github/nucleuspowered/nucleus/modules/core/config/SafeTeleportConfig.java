/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.world.TeleportHelper;

@ConfigSerializable
public class SafeTeleportConfig {

    @Setting
    private int width = TeleportHelper.DEFAULT_WIDTH;

    @Setting
    private int height = TeleportHelper.DEFAULT_HEIGHT;

    public int getWidth() {
        return Math.max(1, this.width);
    }

    public int getHeight() {
        return Math.max(1, this.height);
    }
}
