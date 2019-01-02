/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.services;

import com.flowpowered.math.vector.Vector3d;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Home;
import io.github.nucleuspowered.nucleus.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.internal.LocationData;

import java.util.UUID;

public class NucleusHome extends LocationData implements Home {

    private final UUID owner;

    public NucleusHome(String name, UUID owner, LocationNode node) {
        this(name, owner, node.getWorld(), node.getPosition(), node.getRotation());
    }

    public NucleusHome(String name, UUID owner, UUID world, Vector3d position, Vector3d rotation) {
        super(name, world, position, rotation);
        this.owner = owner;
    }

    @Override public UUID getOwnersUniqueId() {
        return this.owner;
    }
}
