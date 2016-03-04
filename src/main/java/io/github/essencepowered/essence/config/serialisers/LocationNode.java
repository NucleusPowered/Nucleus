/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.config.serialisers;

import com.flowpowered.math.vector.Vector3d;
import io.github.essencepowered.essence.api.exceptions.NoSuchWorldException;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;
import java.util.UUID;

@ConfigSerializable
public class LocationNode {

    @Setting
    private double x;

    @Setting
    private double y;

    @Setting
    private double z;

    @Setting
    private double rotx;

    @Setting
    private double roty;

    @Setting
    private double rotz;

    @Setting
    private UUID world;

    public LocationNode() { }

    public LocationNode(ConfigurationNode locationNode) {
        this.x = locationNode.getNode("x").getDouble();
        this.y = locationNode.getNode("y").getDouble();
        this.z = locationNode.getNode("z").getDouble();
        this.rotx = locationNode.getNode("rotx").getDouble();
        this.roty = locationNode.getNode("roty").getDouble();
        this.rotz = locationNode.getNode("rotz").getDouble();
        this.world = UUID.fromString(locationNode.getNode("world").getString());
    }

    public LocationNode(Location<World> length) {
        this(length, new Vector3d());
    }

    public LocationNode(Location<World> length, Vector3d rotation) {
        this.x = length.getX();
        this.y = length.getY();
        this.z = length.getZ();
        this.rotx = rotation.getX();
        this.roty = rotation.getY();
        this.rotz = rotation.getZ();
        this.world = length.getExtent().getUniqueId();
    }

    public LocationNode(double x, double y, double z, double rotx, double roty, double rotz, UUID world) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotx = rotx;
        this.roty = roty;
        this.rotz = rotz;
        this.world = world;
    }

    /**
     * Populates the selected node with the values.
     *
     * @param cn The node.
     */
    public void populateNode(ConfigurationNode cn) {
        cn.getNode("x").setValue(x);
        cn.getNode("y").setValue(y);
        cn.getNode("z").setValue(z);

        cn.getNode("rotx").setValue(rotx);
        cn.getNode("roty").setValue(roty);
        cn.getNode("rotz").setValue(rotz);

        cn.getNode("world").setValue(world.toString());
    }

    /**
     * Gets a {@link Location} from the node.
     *
     * @return The Location
     * @throws NoSuchWorldException The world does not exist.
     */
    public Location<World> getLocation() throws NoSuchWorldException {
        Optional<World> ow = Sponge.getServer().getWorld(world);

        if (ow.isPresent()) {
            return new Location<World>(ow.get(), x, y, z);
        }

        throw new NoSuchWorldException();
    }

    public Vector3d getRotation() {
        return new Vector3d(rotx, roty, rotz);
    }
}
