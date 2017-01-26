/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.spongedata.manipulators.repair;

import io.github.nucleuspowered.nucleus.spongedata.manipulators.AbstractSignDataBuilder;

public class RepairHandBuilder extends AbstractSignDataBuilder<RepairHandManipulator, ImmutableRepairHandManipulator> {

    public RepairHandBuilder() {
        super(RepairHandManipulator.class, 1);
    }

    @Override public RepairHandManipulator create() {
        return new RepairHandManipulator();
    }
}
