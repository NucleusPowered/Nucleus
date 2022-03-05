/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.module.warp.data;

import io.github.nucleuspowered.nucleus.api.util.data.NamedLocation;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.data.persistence.DataSerializable;

import java.util.Optional;

/**
 * Represents a warp.
 */
public interface Warp extends DataSerializable {

    /**
     * Gets the category for this warp, if it exists.
     *
     * @return The category name.
     */
    Optional<String> getCategory();

    /**
     * Gets the cost of this warp, if the warp has a cost.
     *
     * @return The cost.
     */
    Optional<Double> getCost();

    /**
     * Gets the description for the warp.
     *
     * @return The {@link Component} description, if available.
     */
    Optional<Component> getDescription();

    NamedLocation getNamedLocation();
}
