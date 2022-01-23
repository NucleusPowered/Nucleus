/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.module.home.data;

import io.github.nucleuspowered.nucleus.api.util.data.NamedLocation;

import java.util.UUID;

/**
 * Represents a home in Nucleus.
 */
public interface Home {

    /**
     * The {@link UUID} of the user.
     *
     * @return The {@link UUID}
     */
    UUID getOwnersUniqueId();

    NamedLocation getLocation();

}
