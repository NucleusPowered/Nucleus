/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.events;

import io.github.nucleuspowered.nucleus.api.module.home.event.NucleusHomeEvent;
import io.github.nucleuspowered.nucleus.scaffold.event.AbstractCancelMessageEvent;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

import javax.annotation.Nullable;

public abstract class AbstractHomeEvent extends AbstractCancelMessageEvent implements NucleusHomeEvent {

    private final String name;
    private final User owner;

    private boolean isCancelled = false;
    private final Location<World> location;

    AbstractHomeEvent(String name, User owner, Cause cause, @Nullable Location<World> location) {
        super(cause);
        this.name = name;
        this.owner = owner;
        this.location = location;
    }

    @Override public String getName() {
        return this.name;
    }

    @Override public User getUser() {
        return this.owner;
    }

    @Override public Optional<Location<World>> getLocation() {
        return Optional.ofNullable(this.location);
    }

    @Override public boolean isCancelled() {
        return this.isCancelled;
    }

    @Override public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
    }
}
