/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.configurate.datatypes;

import com.flowpowered.math.vector.Vector3d;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

@ConfigSerializable
public class WarpNode extends LocationNode {

    @Setting("cost")
    private double cost = -1;

    @Setting("category")
    @Nullable
    private String category = null;

    @Setting("description")
    @Nullable
    private Text description = null;

    public WarpNode() {
        super();
    }

    public WarpNode(UUID world, Vector3d length, Vector3d rotation, double cost, String category, Text description) {
        super(world, length, rotation);
        this.cost = cost;
        this.category = category;
        this.description = description;
    }

    public WarpNode(Location<World> location, Vector3d rotation) {
        this(location, rotation, -1);
    }

    private WarpNode(Location<World> location, Vector3d rotation, int cost) {
        super(location, rotation);
        this.cost = cost;
    }

    public WarpNode(Location<World> location) {
        this(location, -1);
    }

    private WarpNode(Location<World> location, int cost) {
        super(location);
        this.cost = cost;
    }

    public double getCost() {
        return this.cost;
    }

    public void setCost(double cost) {
        if (cost < -1) {
            this.cost = -1;
        }

        this.cost = cost;
    }

    public Optional<String> getCategory() {
        return Optional.ofNullable(this.category);
    }

    public void setCategory(@Nullable String category) {
        this.category = category;
    }

    public Text getDescription() {
        return this.description;
    }

    public void setDescription(@Nullable Text description) {
        this.description = description;
    }
}
