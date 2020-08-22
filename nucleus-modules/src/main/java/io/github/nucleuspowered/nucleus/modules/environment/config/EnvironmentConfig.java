/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.environment.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class EnvironmentConfig {

    @Setting(value = "maximum-weather-timespan", comment = "config.environment.maxweathertime")
    private long maximumWeatherTimespan = -1;

    public long getMaximumWeatherTimespan() {
        return this.maximumWeatherTimespan;
    }
}
