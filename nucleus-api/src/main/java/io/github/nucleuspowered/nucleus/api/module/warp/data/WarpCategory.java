/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.module.warp.data;

import net.kyori.adventure.text.Component;
import org.spongepowered.api.data.persistence.DataSerializable;

import java.util.Collection;
import java.util.Optional;

/**
 * Defines a warp category.
 */
public interface WarpCategory extends DataSerializable {

    /**
     * Gets the display name of the category. Defaults to the category name.
     *
     * @return The display name.
     */
    Component getDisplayName();

    /**
     * Gets the defined description of the category, if available.
     *
     * @return An {@link Optional} that might contain a description.
     */
    Optional<Component> getDescription();

    String getId();
}
