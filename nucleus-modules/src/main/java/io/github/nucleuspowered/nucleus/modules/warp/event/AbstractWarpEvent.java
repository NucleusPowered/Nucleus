/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.event;

import io.github.nucleuspowered.nucleus.api.module.warp.event.NucleusWarpEvent;
import io.github.nucleuspowered.nucleus.scaffold.event.AbstractCancelMessageEvent;
import org.spongepowered.api.event.cause.Cause;

public class AbstractWarpEvent extends AbstractCancelMessageEvent implements NucleusWarpEvent {

    private final String name;

    AbstractWarpEvent(Cause cause, String name) {
        super(cause);
        this.name = name;
    }

    @Override public String getName() {
        return this.name;
    }
}
