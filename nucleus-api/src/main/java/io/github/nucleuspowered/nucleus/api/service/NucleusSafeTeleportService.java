/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.service;

import com.flowpowered.math.vector.Vector3d;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportScanner;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.teleport.TeleportHelperFilter;

import java.util.Optional;

public interface NucleusSafeTeleportService {

    TeleportResult teleportPlayer(Player player,
            Location<World> location,
            boolean centreBlock,
            TeleportScanner scanner,
            TeleportHelperFilter filter,
            TeleportHelperFilter... filters);

    TeleportResult teleportPlayer(Player player,
            Location<World> location,
            Vector3d rotation,
            boolean centreBlock,
            TeleportScanner scanner,
            TeleportHelperFilter filter,
            TeleportHelperFilter... filters);

    Optional<Location<World>> getSafeLocation(
            Location<World> location,
            TeleportScanner scanner,
            TeleportHelperFilter filter,
            TeleportHelperFilter... filters);

    Optional<Transform<World>> getSafeTransform(
            Location<World> location,
            Vector3d rotation,
            TeleportScanner scanner,
            TeleportHelperFilter filter,
            TeleportHelperFilter... filters);
}
