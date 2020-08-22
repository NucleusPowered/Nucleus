/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.rtp.kernels;

import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.teleport.TeleportHelperFilter;
import org.spongepowered.api.world.teleport.TeleportHelperFilters;

import javax.annotation.Nullable;

public class SurfaceKernel extends DefaultKernel {

    @Nullable
    @Override
    Location<World> getStartingLocation(Location<World> world) {
        return super.getStartingLocation(
                new Location<>(world.getExtent(), world.getBlockX(), world.getExtent().getBlockMax().getY(), world.getBlockZ()));
    }

    @Override
    TeleportHelperFilter filterToUse() {
        return TeleportHelperFilters.SURFACE_ONLY;
    }

    @Override public String getId() {
        return "nucleus:surface_only";
    }

    @Override public String getName() {
        return "Surface Only Kernel";
    }
}
