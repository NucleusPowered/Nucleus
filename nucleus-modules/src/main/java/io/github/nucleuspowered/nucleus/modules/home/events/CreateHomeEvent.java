/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.events;

import io.github.nucleuspowered.nucleus.api.module.home.event.NucleusHomeEvent;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class CreateHomeEvent extends AbstractHomeEvent implements NucleusHomeEvent.Create {

    public CreateHomeEvent(String name, User owner, Cause cause, Location<World> newLocation) {
        super(name, owner, cause, newLocation);
    }
}
