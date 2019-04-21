/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.teleport.scanners;

import com.flowpowered.math.vector.Vector3i;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportScanner;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.teleport.TeleportHelperFilter;
import org.spongepowered.plugin.meta.util.NonnullByDefault;

import java.util.Optional;

public abstract class VerticalTeleportScanner implements TeleportScanner {

    @Override
    public Optional<Location<World>> scanFrom(
            World world,
            Vector3i position,
            int width,
            int height,
            int floorDistance,
            TeleportHelperFilter filter,
            TeleportHelperFilter... filters) {
        int maxy = world.getBlockMax().getY();
        final int jumps = (height * 2) - 1;

        do {
            Optional<Location<World>> result = Sponge.getTeleportHelper()
                    .getSafeLocation(
                            new Location<>(world, position),
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
        } while (position.getY() < maxy);

        return Optional.empty();
    }

    @NonnullByDefault
    public static class Ascending extends VerticalTeleportScanner {

        @Override
        public String getId() {
            return "nucleus:ascending_scan";
        }

        @Override
        public String getName() {
            return "Nucleus Ascending Scan";
        }

    }

    @NonnullByDefault
    public static class Descending extends VerticalTeleportScanner {

        @Override
        public String getId() {
            return "nucleus:descending_scan";
        }

        @Override
        public String getName() {
            return "Nucleus Descending Scan";
        }

    }
}
