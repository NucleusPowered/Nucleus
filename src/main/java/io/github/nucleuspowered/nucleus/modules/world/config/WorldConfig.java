/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.Optional;

@ConfigSerializable
public class WorldConfig {

    @Setting(value = "default-world-border-diameter", comment = "config.world.defaultborder")
    private long worldBorderDefault = 0;

    @Setting(value = "display-generation-warning", comment = "config.world.gen.warning")
    private boolean displayWarningGeneration = true;

    @Setting(value = "separate-permissions", comment = "config.worlds.separate")
    private boolean separatePermissions = false;

    public boolean isDisplayWarningGeneration() {
        return displayWarningGeneration;
    }

    public Optional<Long> getWorldBorderDefault() {
        if (worldBorderDefault < 1) {
            return Optional.empty();
        }

        return Optional.of(worldBorderDefault);
    }

    public boolean isSeparatePermissions() {
        return separatePermissions;
    }
}
