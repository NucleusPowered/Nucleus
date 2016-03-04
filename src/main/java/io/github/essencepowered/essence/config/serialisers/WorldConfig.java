/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.config.serialisers;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

/**
 * Contains the config entries for any World Config.
 */
@ConfigSerializable
public class WorldConfig {
    @Setting("lock-weather")
    private boolean lockWeather = false;

    public boolean isLockWeather() {
        return lockWeather;
    }

    public void setLockWeather(boolean lockWeather) {
        this.lockWeather = lockWeather;
    }
}
