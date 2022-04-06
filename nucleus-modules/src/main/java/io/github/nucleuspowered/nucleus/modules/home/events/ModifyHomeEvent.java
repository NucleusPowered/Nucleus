/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.events;

import io.github.nucleuspowered.nucleus.api.module.home.data.Home;
import io.github.nucleuspowered.nucleus.api.module.home.event.NucleusHomeEvent;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.world.server.ServerLocation;

import java.util.Optional;

public class ModifyHomeEvent extends AbstractHomeEvent implements NucleusHomeEvent.Modify {

    private final Home home;

    public ModifyHomeEvent(final Cause cause, final Home home, final ServerLocation newLocation) {
        super(home.getLocation().getName(), home.getOwnersUniqueId(), cause, newLocation);
        this.home = home;
    }

    @Override public Home getHome() {
        return this.home;
    }

    @Override public Optional<ServerLocation> getOriginalLocation() {
        return this.home.getLocation().getLocation();
    }
}
