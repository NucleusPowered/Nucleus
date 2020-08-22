/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.inventory.events;

import io.github.nucleuspowered.nucleus.api.module.inventory.NucleusClearInventoryEvent;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;
import org.spongepowered.api.event.user.TargetUserEvent;

public abstract class ClearInventoryEvent extends AbstractEvent implements TargetUserEvent, NucleusClearInventoryEvent {

    private final User target;
    private final Cause cause;
    private final boolean isClearingAll;

    public ClearInventoryEvent(Cause cause, User target, boolean isClearingAll) {
        this.cause = cause;
        this.target = target;
        this.isClearingAll = isClearingAll;
    }

    @Override
    public User getTargetUser() {
        return this.target;
    }

    @Override
    public Cause getCause() {
        return this.cause;
    }

    @Override public boolean isClearingAll() {
        return this.isClearingAll;
    }

    public static class Pre extends ClearInventoryEvent implements NucleusClearInventoryEvent.Pre {

        private boolean cancelled = false;

        public Pre(Cause cause, User target, boolean isClearingAll) {
            super(cause, target, isClearingAll);
        }

        @Override public boolean isCancelled() {
            return this.cancelled;
        }

        @Override public void setCancelled(boolean cancel) {
            this.cancelled = cancel;
        }
    }

    public static class Post extends ClearInventoryEvent implements NucleusClearInventoryEvent.Post {

        public Post(Cause cause, User target, boolean isClearingAll) {
            super(cause, target, isClearingAll);
        }
    }
}
