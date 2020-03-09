/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.back.listeners;

import com.flowpowered.math.vector.Vector3d;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Optional;
import java.util.UUID;

public class OfflineLocation {

    private final Vector3d lastPosition;
    private final Vector3d lastRotation;
    private final UUID lastWorld;

    public OfflineLocation(Vector3d lastPosition, Vector3d lastRotation, UUID lastWorld) {
        this.lastPosition = lastPosition;
        this.lastRotation = lastRotation;
        this.lastWorld = lastWorld;
    }

    public OfflineLocation(Transform<World> location) {
        this(location.getPosition(), location.getRotation(), location.getExtent().getUniqueId());
    }

    public Optional<Transform<World>> getLastLocation() {
        if (this.lastWorld == null || this.lastRotation == null || this.lastPosition == null) {
            return Optional.empty();
        }
        
        Optional<WorldProperties> optionalWorldProperties = Sponge.getServer()
                .getUnloadedWorlds().stream().filter(worldProps -> worldProps.getUniqueId().equals(this.lastWorld))
                .findFirst();

        optionalWorldProperties.ifPresent(worldProperties -> {
            Optional<World> worldOptional = Sponge.getServer().getWorld(worldProperties.getUniqueId());
            if (!worldOptional.isPresent()) Sponge.getServer().loadWorld(worldProperties);
        });

        return Sponge.getServer().getWorld(this.lastWorld)
                .map(world -> new Transform<>(world, this.lastPosition, this.lastRotation));
    }

}
