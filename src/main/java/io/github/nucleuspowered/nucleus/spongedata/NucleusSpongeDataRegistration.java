/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.spongedata;

import io.github.nucleuspowered.nucleus.spongedata.manipulators.repair.ImmutableRepairHandManipulator;
import io.github.nucleuspowered.nucleus.spongedata.manipulators.repair.RepairHandBuilder;
import io.github.nucleuspowered.nucleus.spongedata.manipulators.repair.RepairHandManipulator;
import org.spongepowered.api.GameState;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataManager;

public class NucleusSpongeDataRegistration {

    private NucleusSpongeDataRegistration() {}

    public static void register() {
        if (Sponge.getGame().getState() == GameState.INITIALIZATION) {
            DataManager dataManager = Sponge.getDataManager();
            dataManager.register(RepairHandManipulator.class, ImmutableRepairHandManipulator.class, new RepairHandBuilder());
        }
    }
}
