/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.configurate.datatypes;

import com.flowpowered.math.vector.Vector3d;
import io.github.nucleuspowered.neutrino.annotations.ProcessSetting;
import io.github.nucleuspowered.nucleus.api.exceptions.NoSuchWorldException;
import io.github.nucleuspowered.nucleus.configurate.settingprocessor.WorldMigrationSettingProcessor;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Optional;
import java.util.UUID;

/**
 * We use this class in order to not just accidentally delete data when a world isn't available.
 */
@ConfigSerializable
public class LocationNode {

    @Setting private double x;

    @Setting private double y;

    @Setting private double z;

    @Setting private double rotx;

    @Setting private double roty;

    @Setting private double rotz;

    @Setting
    @ProcessSetting(WorldMigrationSettingProcessor.class)
    private UUID world;

    public LocationNode() { }

    public LocationNode(Location<World> length) {
        this(length, new Vector3d());
    }

    public LocationNode(Location<World> length, Vector3d rotation) {
        this(length.getExtent().getUniqueId(), length.getPosition(), rotation);
    }

    public LocationNode(UUID world, Vector3d length, Vector3d rotation) {
        this.x = length.getX();
        this.y = length.getY();
        this.z = length.getZ();
        this.rotx = rotation.getX();
        this.roty = rotation.getY();
        this.rotz = rotation.getZ();
        this.world = world;
    }

    public LocationNode copy() {
        return new LocationNode(this.world, getPosition(), getRotation());
    }

    public Vector3d getPosition() {
        return new Vector3d(this.x, this.y, this.z);
    }

    public UUID getWorld() {
        return this.world;
    }

    public Tuple<WorldProperties, Vector3d> getLocationIfNotLoaded() throws NoSuchWorldException {
        return Sponge.getServer().getWorldProperties(this.world)
            .map(x -> Tuple.of(x, getPosition())).orElseThrow(NoSuchWorldException::new);
    }

    /**
     * Gets a {@link Location} from the node.
     */
    public Optional<Location<World>> getLocationIfExists() {
        return Sponge.getServer().getWorld(this.world).map(r -> new Location<>(r, this.x, this.y, this.z));
    }

    /**
     * Gets a {@link Location} from the node.
     *
     * @return The Location
     * @throws NoSuchWorldException The world does not exist.
     */
    public Location<World> getLocation() throws NoSuchWorldException {
        return getLocationIfExists().orElseThrow(NoSuchWorldException::new);
    }

    public Vector3d getRotation() {
        return new Vector3d(this.rotx, this.roty, this.rotz);
    }
}
