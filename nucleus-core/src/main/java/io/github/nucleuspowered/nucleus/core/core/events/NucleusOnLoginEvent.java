/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.core.events;

import com.google.common.base.Preconditions;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IUserDataObject;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;
import org.spongepowered.api.world.server.ServerLocation;

import java.util.Optional;

/**
 * Internal only.
 */
public class NucleusOnLoginEvent extends AbstractEvent {

    private final Cause cause;
    private final User user;
    private final IUserDataObject userService;
    private final ServerLocation from;
    @Nullable private ServerLocation to = null;

    public NucleusOnLoginEvent(final Cause cause, final User user, final IUserDataObject userService, final ServerLocation from) {
        Preconditions.checkNotNull(cause);
        Preconditions.checkNotNull(user);
        Preconditions.checkNotNull(userService);
        Preconditions.checkNotNull(from);

        this.cause = cause;
        this.user = user;
        this.userService = userService;
        this.from = from;
    }

    @Override public Cause cause() {
        return this.cause;
    }

    public User getTargetUser() {
        return this.user;
    }

    public IUserDataObject getUserService() {
        return this.userService;
    }

    public User getUser() {
        return this.user;
    }

    public ServerLocation getFrom() {
        return this.from;
    }

    public Optional<ServerLocation> getTo() {
        return Optional.ofNullable(this.to);
    }

    public void setTo(@Nullable final ServerLocation to) {
        this.to = to;
    }
}
