/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.events;

import io.github.nucleuspowered.nucleus.api.module.home.data.Home;
import io.github.nucleuspowered.nucleus.api.module.home.event.NucleusHomeEvent;
import org.spongepowered.api.event.Cause;

import java.util.UUID;

public class UseHomeEvent extends AbstractHomeEvent implements NucleusHomeEvent.Use {

    private final UUID targetUser;
    private final Home home;

    public UseHomeEvent(final Cause cause, final UUID targetUser, final Home home) {
        super(home.getLocation().getName(), home.getOwnersUniqueId(), cause, home.getLocation().getLocation().orElse(null));
        this.targetUser = targetUser;
        this.home = home;
    }

    @Override
    public UUID getTeleportingUser() {
        return this.targetUser;
    }

    @Override
    public Home getHome() {
        return this.home;
    }
}
