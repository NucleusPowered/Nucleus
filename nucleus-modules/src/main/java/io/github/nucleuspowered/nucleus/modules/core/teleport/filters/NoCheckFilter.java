/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.teleport.filters;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.teleport.TeleportHelperFilter;
import org.spongepowered.plugin.meta.util.NonnullByDefault;

@NonnullByDefault
public class NoCheckFilter implements TeleportHelperFilter {

    @Override
    public Tristate isValidLocation(World world, Vector3i position) {
        return Tristate.TRUE;
    }

    @Override
    public boolean isSafeFloorMaterial(BlockState blockState) {
        return true;
    }

    @Override
    public boolean isSafeBodyMaterial(BlockState blockState) {
        return true;
    }

    @Override
    public String getId() {
        return "nucleus:no_check";
    }

    @Override
    public String getName() {
        return "Nucleus No Check filter";
    }
}
