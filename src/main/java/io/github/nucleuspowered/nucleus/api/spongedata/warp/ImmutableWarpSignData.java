/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.spongedata.warp;

import org.spongepowered.api.data.manipulator.ImmutableDataManipulator;
import org.spongepowered.api.data.value.immutable.ImmutableBoundedValue;
import org.spongepowered.api.data.value.immutable.ImmutableValue;

import java.util.Optional;

/**
 * An immutable copy of {@link WarpSignData}
 */
public interface ImmutableWarpSignData extends ImmutableDataManipulator<ImmutableWarpSignData, WarpSignData> {

    ImmutableValue<String> warpName();

    ImmutableValue<Optional<String>> permission();

    ImmutableBoundedValue<Integer> warmupTime();

    ImmutableBoundedValue<Integer> cost();
}
