/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.event;

import io.github.nucleuspowered.nucleus.api.module.warp.data.Warp;
import io.github.nucleuspowered.nucleus.api.module.warp.event.NucleusWarpEvent;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class UseWarpEvent extends AbstractWarpEvent implements NucleusWarpEvent.Use {

    private final User user;
    private final Warp warp;

    public UseWarpEvent(Cause cause, User user, Warp warp) {
        super(cause, warp.getName());
        this.user = user;
        this.warp = warp;
    }

    @Override public Warp getWarp() {
        return this.warp;
    }

    @Override public Location<World> getLocation() {
        return this.warp.getLocation().get();
    }

    @Override public User getTargetUser() {
        return this.user;
    }
}
