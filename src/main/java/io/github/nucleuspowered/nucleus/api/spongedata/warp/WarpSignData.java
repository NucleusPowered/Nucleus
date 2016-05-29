/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.spongedata.warp;

import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.value.mutable.MutableBoundedValue;
import org.spongepowered.api.data.value.mutable.OptionalValue;
import org.spongepowered.api.data.value.mutable.Value;

/**
 * Data that represents a sign that can warp a player.
 */
public interface WarpSignData extends DataManipulator<WarpSignData, ImmutableWarpSignData> {

    /**
     * Gets the name of the warp that this data points to.
     *
     * @return The name of the warp.
     */
    Value<String> warpName();

    /**
     * Gets the permission that is required to use this warp, if one is required.
     *
     * @return The permission if one is required.
     */
    OptionalValue<String> permission();

    /**
     * The warmup, in seconds, for the warp.
     *
     * @return The warmup, in seconds.
     */
    MutableBoundedValue<Integer> warmupTime();
}
