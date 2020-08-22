/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.events;

import io.github.nucleuspowered.nucleus.api.module.jail.event.NucleusJailEvent;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.time.Duration;
import java.util.Optional;

import javax.annotation.Nullable;

@NonnullByDefault
public abstract class JailEvent extends AbstractEvent implements NucleusJailEvent {

    private final User targetUser;
    private final Cause cause;

    private JailEvent(User targetUser, Cause cause) {
        this.targetUser = targetUser;
        this.cause = cause;
    }

    @Override public User getTargetUser() {
        return this.targetUser;
    }

    @Override public Cause getCause() {
        return this.cause;
    }

    public static class Jailed extends JailEvent implements NucleusJailEvent.Jailed {

        private final String jailName;
        private final Text reason;
        @Nullable private final Duration duration;

        public Jailed(User targetUser, Cause cause, String jailName, Text reason, @Nullable Duration duration) {
            super(targetUser, cause);
            this.jailName = jailName;
            this.reason = reason;
            this.duration = duration;
        }

        @Override public String getJailName() {
            return this.jailName;
        }

        @Override public Optional<Duration> getDuration() {
            return Optional.ofNullable(this.duration);
        }

        @Override public Text getReason() {
            return this.reason;
        }
    }

    public static class Unjailed extends JailEvent implements NucleusJailEvent.Unjailed {

        public Unjailed(User targetUser, Cause cause) {
            super(targetUser, cause);
        }
    }
}
