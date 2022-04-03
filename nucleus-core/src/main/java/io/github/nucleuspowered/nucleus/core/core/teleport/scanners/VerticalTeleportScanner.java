/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.core.teleport.scanners;

import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanner;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.teleport.TeleportHelperFilter;

import java.util.Optional;

public abstract class VerticalTeleportScanner implements TeleportScanner {

    private final boolean isAscending;

    protected VerticalTeleportScanner(final boolean isAscending) {
        this.isAscending = isAscending;
    }

    @Override
    public Optional<ServerLocation> scanFrom(
            final ServerWorld world,
            Vector3i position,
            final int width,
            final int height,
            final int floorDistance,
            final TeleportHelperFilter filter,
            final TeleportHelperFilter... filters) {
        final int maxy = world.max().y();
        final int jumps = ((height * 2) - 1) * (this.isAscending ? 1 : -1);

        do {
            final Optional<ServerLocation> result = Sponge.server().teleportHelper()
                    .findSafeLocation(
                            ServerLocation.of(world, position),
                            height,
                            width,
                            floorDistance,
                            filter,
                            filters
                    );
            if (result.isPresent()) {
                return result;
            }

            position = position.add(0, jumps, 0);
        } while (position.y() < maxy);

        return Optional.empty();
    }

    public static class Ascending extends VerticalTeleportScanner {

        public static final ResourceKey KEY = ResourceKey.of("nucleus", "ascending_scan");

        public Ascending() {
            super(true);
        }

    }

    public static class Descending extends VerticalTeleportScanner {

        public static final ResourceKey KEY = ResourceKey.of("nucleus", "descending_scan");

        public Descending() {
            super(false);
        }

    }
}
