/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.teleport;

import org.spongepowered.api.util.generator.dummy.DummyObjectProvider;
import org.spongepowered.api.world.teleport.TeleportHelperFilter;

public final class NucleusTeleportFilters {

    public static final TeleportHelperFilter NO_CHECK = DummyObjectProvider.createFor(TeleportHelperFilter.class, "NO_CHECK");

    public static final TeleportHelperFilter WALL_CHECK = DummyObjectProvider.createFor(TeleportHelperFilter.class, "WALL_CHECK");

}
