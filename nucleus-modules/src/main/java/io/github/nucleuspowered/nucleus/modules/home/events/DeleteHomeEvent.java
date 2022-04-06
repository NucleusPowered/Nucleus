/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.events;

import io.github.nucleuspowered.nucleus.api.module.home.data.Home;
import io.github.nucleuspowered.nucleus.api.module.home.event.NucleusHomeEvent;
import org.spongepowered.api.event.Cause;

public class DeleteHomeEvent extends AbstractHomeEvent implements NucleusHomeEvent.Delete {

    private final Home home;

    public DeleteHomeEvent(final Cause cause, final Home home) {
        super(home.getLocation().getName(), home.getOwnersUniqueId(), cause, home.getLocation().getLocation().orElse(null));
        this.home = home;
    }

    @Override public Home getHome() {
        return this.home;
    }
}
