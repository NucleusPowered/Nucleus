/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.teleport;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.TeleportHelper;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.teleport.TeleportHelperFilter;

import java.util.Optional;

/**
 * Scans {@link Location}s for safe teleport locations.
 */
public interface TeleportScanner extends CatalogType {

    default Optional<Location<World>> scanFrom(
            World world,
            Vector3i position,
            TeleportHelperFilter filter,
            TeleportHelperFilter... filters) {
        return scanFrom(
                world,
                position,
                TeleportHelper.DEFAULT_HEIGHT,
                TeleportHelper.DEFAULT_WIDTH,
                TeleportHelper.DEFAULT_FLOOR_CHECK_DISTANCE,
                filter,
                filters
        );
    }

    Optional<Location<World>> scanFrom(
            World world,
            Vector3i position,
            int height,
            int width,
            int floor,
            TeleportHelperFilter filter,
            TeleportHelperFilter... filters);

}
