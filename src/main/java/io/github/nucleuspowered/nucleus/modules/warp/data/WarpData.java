/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.data;

import com.flowpowered.math.vector.Vector3d;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Warp;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Optional;
import java.util.UUID;

public class WarpData implements Warp {

    private final String category;
    private final Double cost;
    private final Text description;
    private final UUID worldPropertiesUUID;
    private final Vector3d position;
    private final Vector3d rotation;
    private final String name;

    public WarpData(String category,
                    double cost,
                    Text description,
                    UUID worldPropertiesUUID,
                    Vector3d position,
                    Vector3d rotation,
                    String name) {
        this.category = category;
        this.cost = cost == 0 ? null : cost;
        this.description = description;
        this.worldPropertiesUUID = worldPropertiesUUID;
        this.position = position;
        this.rotation = rotation;
        this.name = name;
    }

    @Override
    public Optional<String> getCategory() {
        return Optional.ofNullable(this.category);
    }

    @Override
    public Optional<Double> getCost() {
        return Optional.ofNullable(this.cost);
    }

    @Override
    public Optional<Text> getDescription() {
        return Optional.ofNullable(this.description);
    }

    @Override
    public UUID getWorldUUID() {
        return this.worldPropertiesUUID;
    }

    @Override
    public Optional<WorldProperties> getWorldProperties() {
        return Sponge.getServer().getWorldProperties(this.worldPropertiesUUID);
    }

    @Override
    public Vector3d getRotation() {
        return this.rotation;
    }

    @Override
    public Vector3d getPosition() {
        return this.position;
    }

    @Override
    public Optional<Location<World>> getLocation() {
        return Sponge.getServer().getWorld(this.worldPropertiesUUID).map(x -> new Location<>(x, this.position));
    }

    @Override
    public Optional<Transform<World>> getTransform() {
        return Sponge.getServer().getWorld(this.worldPropertiesUUID)
                .map(x -> new Transform<>(x, this.position, this.rotation));
    }

    @Override
    public String getName() {
        return this.name;
    }
}
