/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.util.data;

import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.persistence.DataSerializable;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.server.storage.ServerWorldProperties;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.math.vector.Vector3d;

import java.util.Optional;

/**
 * Represents a location that has a name attached to it.
 */
public interface NamedLocation extends DataSerializable {

    /**
     * Gets the world {@link ResourceKey} that this location targets
     *
     * @return The world identifier
     */
    ResourceKey getWorldResourceKey();

    /**
     * Gets the {@link ServerWorld} that this location points to, if the world exists.
     *
     * @return The World Properties
     */
    Optional<ServerWorld> getWorld();

    /**
     * Gets the rotation.
     *
     * @return The rotation
     */
    Vector3d getRotation();

    /**
     * Gets the position.
     *
     * @return The position
     */
    Vector3d getPosition();

    /**
     * Gets the {@link Location} if the world is loaded.
     *
     * @return The {@link Location} if the world is loaded.
     */
    Optional<ServerLocation> getLocation();

    /**
     * Gets the name of the location.
     *
     * @return The name
     */
    String getName();
}
