/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.catalogkeys;

import org.spongepowered.api.util.generator.dummy.DummyObjectProvider;
import org.spongepowered.api.world.teleport.TeleportHelperFilter;

public final class NucleusTeleportHelperFilters {

    /**
     * Returns the location that is passed into the filter.
     */
    public final static TeleportHelperFilter NO_CHECK = DummyObjectProvider.createFor(TeleportHelperFilter.class, "NO_CHECK");

    /**
     * Returns a location that is not a wall.
     */
    public final static TeleportHelperFilter WALL_CHECK = DummyObjectProvider.createFor(TeleportHelperFilter.class, "WALL_CHECK");

}
