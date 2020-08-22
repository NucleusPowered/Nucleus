/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.message.events;

import io.github.nucleuspowered.nucleus.util.CauseStackHelper;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;

public class InternalNucleusHelpOpEvent extends AbstractEvent implements Cancellable {

    private final String message;
    private final Cause cause;
    private boolean isCancelled = false;

    public InternalNucleusHelpOpEvent(CommandSource from, String message) {
        this.cause = CauseStackHelper.createCause(from);
        this.message = message;
    }

    @Override
    public Cause getCause() {
        return this.cause;
    }

    @Override
    public boolean isCancelled() {
        return this.isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
    }

    public String getMessage() {
        return this.message;
    }

}
