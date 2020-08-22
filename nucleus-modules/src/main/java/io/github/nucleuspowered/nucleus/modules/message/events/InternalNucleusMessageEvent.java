/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.message.events;

import io.github.nucleuspowered.nucleus.api.module.message.event.NucleusMessageEvent;
import io.github.nucleuspowered.nucleus.util.CauseStackHelper;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
public class InternalNucleusMessageEvent extends AbstractEvent implements NucleusMessageEvent {

    private final Cause cause;
    private final CommandSource from;
    private final CommandSource to;
    private final String message;
    private boolean isCancelled = false;

    public InternalNucleusMessageEvent(CommandSource from, CommandSource to, String message) {
        this.cause = CauseStackHelper.createCause(from);
        this.from = from;
        this.to = to;
        this.message = message;
    }

    @Override
    public Cause getCause() {
        return this.cause;
    }

    @Override
    public CommandSource getSender() {
        return this.from;
    }

    @Override
    public CommandSource getRecipient() {
        return this.to;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public boolean isCancelled() {
        return this.isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
    }
}
