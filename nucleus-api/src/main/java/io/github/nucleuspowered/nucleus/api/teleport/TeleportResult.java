/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.teleport;

import org.spongepowered.api.CatalogType;

public interface TeleportResult extends CatalogType {

    boolean isSuccessful();

}
