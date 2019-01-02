/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.nucleusdata;

import com.flowpowered.math.vector.Vector3d;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Optional;
import java.util.UUID;

/**
 * Represents a location that has a name attached to it.
 */
public interface NamedLocation {

    /**
     * Gets the world {@link UUID} that this location targets
     *
     * @return The world UUID
     */
    UUID getWorldUUID();

    /**
     * Gets the {@link WorldProperties} that this location points to, if the world exists.
     *
     * @return The World Properties
     */
    Optional<WorldProperties> getWorldProperties();

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
    Optional<Location<World>> getLocation();

    /**
     * Gets the {@link Transform} if the world is loaded.
     *
     * @return The {@link Transform} if the world is loaded.
     */
    Optional<Transform<World>> getTransform();

    /**
     * Gets the name of the location.
     *
     * @return The name
     */
    String getName();
}
