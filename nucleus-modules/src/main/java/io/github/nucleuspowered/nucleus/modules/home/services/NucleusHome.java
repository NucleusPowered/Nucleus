/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.services;

import org.spongepowered.api.ResourceKey;
import org.spongepowered.math.vector.Vector3d;
import io.github.nucleuspowered.nucleus.api.module.home.data.Home;
import io.github.nucleuspowered.nucleus.core.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.core.datatypes.NucleusNamedLocation;

import java.util.UUID;

public class NucleusHome implements Home {

    private final UUID owner;

    public NucleusHome(final String name, final UUID owner, final LocationNode node) {
        this(name, owner, node.getWorld(), node.getPosition(), node.getRotation());
    }

    public NucleusHome(final String name, final UUID owner, final ResourceKey world, final Vector3d position, final Vector3d rotation) {
        super(name, world, position, rotation);
        this.owner = owner;
    }

    @Override public UUID getOwnersUniqueId() {
        return this.owner;
    }
}
