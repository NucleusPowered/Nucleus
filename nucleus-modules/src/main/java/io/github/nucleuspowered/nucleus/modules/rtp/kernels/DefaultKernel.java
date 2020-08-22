/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.rtp.kernels;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.module.rtp.NucleusRTPService;
import io.github.nucleuspowered.nucleus.api.module.rtp.kernel.RTPKernel;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.TeleportHelper;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.teleport.TeleportHelperFilter;
import org.spongepowered.api.world.teleport.TeleportHelperFilters;

import java.util.Optional;

import javax.annotation.Nullable;

public class DefaultKernel implements RTPKernel {

    public static final DefaultKernel INSTANCE = new DefaultKernel();

    @Override
    public Optional<Location<World>> getLocation(@Nullable Location<World> currentLocation, World target, NucleusRTPService.RTPOptions options) {
        // from world spawn
        Vector3d location;
        int count = 25;
        do {
            if (--count < 0) {
                // We found nothing in the timeframe.
                return Optional.empty();
            }

            location = KernelHelper.INSTANCE.getLocationWithOffset(getCentralLocation(currentLocation, target), options);
        } while (!Util.isLocationInWorldBorder(location.toDouble(), target));

        Location<World> worldLocation = getStartingLocation(new Location<>(target, location));
        if (worldLocation == null) {
            return Optional.empty();
        }

        Optional<Location<World>> targetLocation = Sponge.getTeleportHelper().getSafeLocation(worldLocation,
                TeleportHelper.DEFAULT_HEIGHT,
                TeleportHelper.DEFAULT_WIDTH,
                TeleportHelper.DEFAULT_FLOOR_CHECK_DISTANCE,
                TeleportHelperFilters.CONFIG,
                filterToUse());
        if (targetLocation.isPresent()) {
            // Is it in the world border?
            if (!Util.isLocationInWorldBorder(worldLocation)
                    || options.prohibitedBiomes().contains(worldLocation.getBiome())
                    || options.minHeight() > worldLocation.getBlockY()
                    || options.maxHeight() < worldLocation.getBlockY()) {
                return Optional.empty();
            }

            return verifyLocation(targetLocation.get()) ? targetLocation : Optional.empty();
        }

        return Optional.empty();
    }

    TeleportHelperFilter filterToUse() {
        return TeleportHelperFilters.DEFAULT;
    }

    Vector3i getCentralLocation(@Nullable Location<World> currentLocation, World world) {
        return world.getSpawnLocation().getBlockPosition();
    }

    @Nullable Location<World> getStartingLocation(Location<World> world) {
        while (world.getBlockType() == BlockTypes.AIR) {
            if (world.getY() < 1) {
                return null;
            }
            world = world.sub(0, 1, 0);
        }

        return world;
    }

    boolean verifyLocation(Location<World> world) {
        return true;
    }

    @Override public String getId() {
        return "nucleus:default";
    }

    @Override public String getName() {
        return "Default Kernel";
    }
}
