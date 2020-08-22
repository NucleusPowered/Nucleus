/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.event;

import io.github.nucleuspowered.nucleus.api.module.warp.data.Warp;
import io.github.nucleuspowered.nucleus.api.module.warp.event.NucleusWarpEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

@NonnullByDefault
public class DeleteWarpEvent extends AbstractWarpEvent implements NucleusWarpEvent.Delete {

    private final Warp warp;

    public DeleteWarpEvent(Cause cause, Warp warp) {
        super(cause, warp.getName());
        this.warp = warp;
    }

    @Override public Warp getWarp() {
        return this.warp;
    }

    @Override public Optional<Location<World>> getLocation() {
        return this.warp.getLocation();
    }
}
