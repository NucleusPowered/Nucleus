/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.api.data;

import io.github.essencepowered.essence.Essence;

/**
 * Represents data held about a world in {@link Essence}.
 */
public interface EssenceWorld {

    /**
     * Gets whether the weather has been locked in this world.
     *
     * @return <code>true</code> if the weather has been locked.
     */
    boolean isLockWeather();

    /**
     * Sets whether the weather has been locked in this world.
     *
     * @param lockWeather <code>true</code> if the weather should be locked, <code>false</code> otherwise.
     */
    void setLockWeather(boolean lockWeather);
}
