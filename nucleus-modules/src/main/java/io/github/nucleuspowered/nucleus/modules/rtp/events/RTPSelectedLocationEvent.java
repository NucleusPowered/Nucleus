/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.rtp.events;

import io.github.nucleuspowered.nucleus.api.module.rtp.event.NucleusRTPEvent;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

@NonnullByDefault
public class RTPSelectedLocationEvent extends AbstractEvent implements NucleusRTPEvent.SelectedLocation {

    private final Location<World> location;
    private final Player player;
    private final Cause cause;
    private boolean isCancelled = false;

    public RTPSelectedLocationEvent(Location<World> location, Player player, Cause cause) {
        this.location = location;
        this.player = player;
        this.cause = cause;
    }

    @Override
    public Location<World> getLocation() {
        return this.location;
    }

    @Override
    public boolean isCancelled() {
        return this.isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
    }

    @Override
    public Player getTargetEntity() {
        return this.player;
    }

    @Override
    public Cause getCause() {
        return this.cause;
    }
}
