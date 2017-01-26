/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.sign.signlisteners;

import io.github.nucleuspowered.nucleus.internal.signs.SignDataListenerBase;
import io.github.nucleuspowered.nucleus.spongedata.manipulators.repair.RepairHandManipulator;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.entity.living.player.Player;

public class RepairSignListener extends SignDataListenerBase<RepairHandManipulator> {

    @Override public Class<RepairHandManipulator> getDataClass() {
        return RepairHandManipulator.class;
    }

    @Override protected boolean onBreak(Sign sign, RepairHandManipulator data, Player player) {
        return false;
    }

    @Override protected boolean onInteract(Sign sign, RepairHandManipulator data, Player player) {
        return false;
    }
}
