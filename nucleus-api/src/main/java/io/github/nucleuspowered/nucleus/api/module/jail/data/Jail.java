/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.module.jail.data;

import io.github.nucleuspowered.nucleus.api.util.data.NamedLocation;
import org.spongepowered.api.data.persistence.DataSerializable;

/**
 * Represents a jail
 */
public interface Jail extends DataSerializable {

    NamedLocation getLocation();

}
