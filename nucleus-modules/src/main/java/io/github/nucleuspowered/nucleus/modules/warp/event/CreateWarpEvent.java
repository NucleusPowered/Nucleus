/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.event;

import io.github.nucleuspowered.nucleus.api.module.warp.event.NucleusWarpEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class CreateWarpEvent extends AbstractWarpEvent implements NucleusWarpEvent.Create {

    private final Location<World> location;

    public CreateWarpEvent(Cause cause, String name, Location<World> location) {
        super(cause, name);
        this.location = location;
    }

    @Override public Location<World> getLocation() {
        return this.location;
    }
}
