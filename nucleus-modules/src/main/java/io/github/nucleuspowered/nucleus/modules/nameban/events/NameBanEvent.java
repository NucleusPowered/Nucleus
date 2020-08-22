/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.nameban.events;

import io.github.nucleuspowered.nucleus.api.module.nameban.event.NucleusNameBanEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;

public abstract class NameBanEvent extends AbstractEvent implements NucleusNameBanEvent {

    private final String entry;
    private final Cause cause;
    private final String reason;

    private NameBanEvent(String entry, String reason, Cause cause) {
        this.entry = entry;
        this.reason = reason;
        this.cause = cause;
    }

    @Override public String getEntry() {
        return this.entry;
    }

    @Override public String getReason() {
        return this.reason;
    }

    @Override public Cause getCause() {
        return this.cause;
    }

    public static class Banned extends NameBanEvent {

        public Banned(String entry, String reason, Cause cause) {
            super(entry, reason, cause);
        }
    }

    public static class Unbanned extends NameBanEvent {

        public Unbanned(String entry, String reason, Cause cause) {
            super(entry, reason, cause);
        }
    }
}
