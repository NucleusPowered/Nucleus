/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.module.home.data;

import io.github.nucleuspowered.nucleus.api.util.data.NamedLocation;
import org.spongepowered.api.data.persistence.DataSerializable;

import java.util.UUID;

/**
 * Represents a home in Nucleus.
 */
public interface Home extends DataSerializable {

    /**
     * The {@link UUID} of the user.
     *
     * @return The {@link UUID}
     */
    UUID getOwnersUniqueId();

    /**
     * Gets the location and name of this home.
     *
     * @return The {@link NamedLocation}
     */
    NamedLocation getLocation();

}
