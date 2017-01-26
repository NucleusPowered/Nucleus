/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.spongedata.manipulators.repair;

import io.github.nucleuspowered.nucleus.spongedata.manipulators.AbstractSignManipulator;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Optional;

import javax.annotation.Nullable;

@NonnullByDefault
public class RepairHandManipulator extends AbstractSignManipulator.Simple<RepairHandManipulator, ImmutableRepairHandManipulator> {

    public RepairHandManipulator() {
        super();
    }

    public RepairHandManipulator(@Nullable String permission, double cost) {
        super(permission, cost);
    }

    @Override public Optional<RepairHandManipulator> fill(DataHolder dataHolder, MergeFunction overlap) {
        return fillInternal(RepairHandManipulator.class, dataHolder, overlap);
    }

    @Override public Optional<RepairHandManipulator> from(DataContainer container) {
        fromInternal(container);
        return Optional.of(this);
    }

    @Override public RepairHandManipulator copy() {
        return new RepairHandManipulator(permission, cost);
    }

    @Override public ImmutableRepairHandManipulator asImmutable() {
        return new ImmutableRepairHandManipulator(permission, cost);
    }

}
