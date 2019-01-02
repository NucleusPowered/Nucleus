/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal;

import com.flowpowered.math.vector.Vector3d;
import io.github.nucleuspowered.nucleus.api.nucleusdata.NamedLocation;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.UUID;

public class LocationData implements NamedLocation {

    private final String warpName;
    private final UUID worldUUID;
    @Nullable private final WorldProperties worldProperties;
    private final Vector3d position;
    private final Vector3d rotation;

    public LocationData(String name, UUID world, Vector3d position, Vector3d rotation) {
        this.rotation = rotation;
        this.position = position;
        this.warpName = name;
        this.worldUUID = world;
        this.worldProperties = Sponge.getServer().getWorldProperties(this.worldUUID).orElse(null);
    }

    public String getName() {
        return this.warpName;
    }

    @Override
    public UUID getWorldUUID() {
        return this.worldUUID;
    }

    @Override public Optional<WorldProperties> getWorldProperties() {
        return Optional.ofNullable(this.worldProperties);
    }

    public Vector3d getRotation() {
        return this.rotation;
    }

    @Override public Vector3d getPosition() {
        return this.position;
    }

    @Override public Optional<Location<World>> getLocation() {
        Optional<World> optional = Sponge.getServer().getWorld(this.worldUUID);
        return optional.map(world -> new Location<>(world, this.position));
    }

    @Override public Optional<Transform<World>> getTransform() {
        Optional<Location<World>> olw = getLocation();
        return olw.map(worldLocation -> new Transform<>(worldLocation.getExtent(), this.position, this.rotation));
    }

    public String toLocationString() {
        if (this.worldProperties == null) {
            return MessageFormat.format("name: {0}, no location", this.warpName);
        }

        return MessageFormat.format("name: {0}, world: {1}, x: {2}, y: {3}, z: {4}", this.warpName, this.worldProperties.getWorldName(),
            (int) this.position.getX(), (int) this.position.getY(), (int) this.position.getZ());
    }
}
