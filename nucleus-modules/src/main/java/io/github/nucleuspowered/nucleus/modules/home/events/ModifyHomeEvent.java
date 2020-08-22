/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.events;

import io.github.nucleuspowered.nucleus.api.module.home.data.Home;
import io.github.nucleuspowered.nucleus.api.module.home.event.NucleusHomeEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

public class ModifyHomeEvent extends AbstractHomeEvent implements NucleusHomeEvent.Modify {

    private final Home home;

    public ModifyHomeEvent(Cause cause, Home home, Location<World> newLocation) {
        super(home.getName(), home.getUser(), cause, newLocation);
        this.home = home;
    }

    @Override public Home getHome() {
        return this.home;
    }

    @Override public Optional<Location<World>> getOriginalLocation() {
        return this.home.getLocation();
    }
}
