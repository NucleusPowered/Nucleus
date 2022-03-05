/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.event;

import io.github.nucleuspowered.nucleus.api.module.warp.data.Warp;
import io.github.nucleuspowered.nucleus.api.module.warp.event.NucleusWarpEvent;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.world.server.ServerLocation;

import java.util.UUID;

public class UseWarpEvent extends AbstractWarpEvent implements NucleusWarpEvent.Use {

    private final UUID user;
    private final Warp warp;

    public UseWarpEvent(final Cause cause, final UUID user, final Warp warp) {
        super(cause, warp.getNamedLocation().getName());
        this.user = user;
        this.warp = warp;
    }

    @Override public Warp getWarp() {
        return this.warp;
    }

    @Override public ServerLocation getLocation() {
        return this.warp.getNamedLocation().getLocation().get();
    }

    @Override public UUID getTargetUser() {
        return this.user;
    }
}
