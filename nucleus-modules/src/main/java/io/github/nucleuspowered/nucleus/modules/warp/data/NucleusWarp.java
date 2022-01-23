/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.data;

import io.github.nucleuspowered.nucleus.api.module.warp.data.Warp;
import io.github.nucleuspowered.nucleus.core.datatypes.NucleusNamedLocation;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.math.vector.Vector3d;

import java.util.Optional;

public class NucleusWarp extends NucleusNamedLocation implements Warp {

    private final String category;
    private final Double cost;
    private final Component description;

    public NucleusWarp(final String category,
                       final double cost,
                       final Component description,
                       final ResourceKey worldKey,
                       final Vector3d position,
                       final Vector3d rotation,
                       final String name) {
        super(name, worldKey, position, rotation);
        this.category = category;
        this.cost = cost == 0 ? null : cost;
        this.description = description;
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
    public Optional<Component> getDescription() {
        return Optional.ofNullable(this.description);
    }

}
