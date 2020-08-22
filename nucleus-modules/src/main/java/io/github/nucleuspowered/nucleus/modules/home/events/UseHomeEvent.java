/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.events;

import io.github.nucleuspowered.nucleus.api.module.home.data.Home;
import io.github.nucleuspowered.nucleus.api.module.home.event.NucleusHomeEvent;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
public class UseHomeEvent extends AbstractHomeEvent implements NucleusHomeEvent.Use {

    private final User targetUser;
    private final Home home;

    public UseHomeEvent(Cause cause, User targetUser, Home home) {
        super(home.getName(), home.getUser(), cause, home.getLocation().orElse(null));
        this.targetUser = targetUser;
        this.home = home;
    }

    @Override public User getTargetUser() {
        return this.targetUser;
    }

    @Override public Home getHome() {
        return this.home;
    }
}
