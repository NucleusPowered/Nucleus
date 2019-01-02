/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.events;

import com.google.common.base.Preconditions;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IUserDataObject;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;
import org.spongepowered.api.event.user.TargetUserEvent;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.World;

import java.util.Optional;

import javax.annotation.Nullable;

/**
 * Internal only.
 */
@NonnullByDefault
public class NucleusOnLoginEvent extends AbstractEvent implements TargetUserEvent {

    private final Cause cause;
    private final User user;
    private final IUserDataObject userService;
    private final Transform<World> from;
    @Nullable private Transform<World> to = null;

    public NucleusOnLoginEvent(Cause cause, User user, IUserDataObject userService, Transform<World> from) {
        Preconditions.checkNotNull(cause);
        Preconditions.checkNotNull(user);
        Preconditions.checkNotNull(userService);
        Preconditions.checkNotNull(from);

        this.cause = cause;
        this.user = user;
        this.userService = userService;
        this.from = from;
    }

    @Override public Cause getCause() {
        return this.cause;
    }

    @Override public User getTargetUser() {
        return this.user;
    }

    public IUserDataObject getUserService() {
        return this.userService;
    }

    public User getUser() {
        return this.user;
    }

    public Transform<World> getFrom() {
        return this.from;
    }

    public Optional<Transform<World>> getTo() {
        return Optional.ofNullable(this.to);
    }

    public void setTo(@Nullable Transform<World> to) {
        this.to = to;
    }
}
