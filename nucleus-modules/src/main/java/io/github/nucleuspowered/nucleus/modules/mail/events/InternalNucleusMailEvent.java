/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mail.events;

import io.github.nucleuspowered.nucleus.api.module.mail.event.NucleusMailEvent;
import io.github.nucleuspowered.nucleus.util.CauseStackHelper;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Optional;

import javax.annotation.Nullable;

@NonnullByDefault
public class InternalNucleusMailEvent extends AbstractEvent implements NucleusMailEvent {

    @Nullable private final User from;
    private final User to;
    private final String message;
    private final Cause cause;
    private boolean cancelled = false;

    public InternalNucleusMailEvent(@Nullable User from, User to, String message) {
        this.cause = CauseStackHelper.createCause(from == null ? Sponge.getServer().getConsole() : from);
        this.from = from;
        this.to = to;
        this.message = message;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public Cause getCause() {
        return this.cause;
    }

    @Override
    public Optional<User> getSender() {
        return Optional.ofNullable(this.from);
    }

    @Override
    public User getRecipient() {
        return this.to;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
