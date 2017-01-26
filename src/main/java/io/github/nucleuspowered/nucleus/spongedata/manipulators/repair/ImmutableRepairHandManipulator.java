/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.spongedata.manipulators.repair;

import io.github.nucleuspowered.nucleus.spongedata.manipulators.AbstractImmutableSignManipulator;

import javax.annotation.Nullable;

public class ImmutableRepairHandManipulator extends AbstractImmutableSignManipulator.Simple<ImmutableRepairHandManipulator, RepairHandManipulator> {

    public ImmutableRepairHandManipulator(@Nullable String permission, @Nullable Double cost) {
        super(permission, cost);
    }

    @Override public RepairHandManipulator asMutable() {
        return new RepairHandManipulator(permission, cost);
    }
}
