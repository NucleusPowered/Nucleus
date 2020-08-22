/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.afk.events;

import com.google.common.base.Preconditions;
import io.github.nucleuspowered.nucleus.api.module.afk.event.NucleusAFKEvent;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.entity.living.humanoid.player.TargetPlayerEvent;
import org.spongepowered.api.event.impl.AbstractEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Optional;

import javax.annotation.Nullable;

@NonnullByDefault
public abstract class AFKEvents extends AbstractEvent implements TargetPlayerEvent, NucleusAFKEvent {

    private final Player target;
    private final Cause cause;
    private final MessageChannel original;
    private MessageChannel channel;
    @Nullable private final Text originalMessage;
    @Nullable private Text message;

    AFKEvents(Player target, @Nullable Text message, @Nullable MessageChannel original) {
        this(target, message, original, Cause.of(EventContext.builder().add(EventContextKeys.OWNER, target).build(), target));
    }

    AFKEvents(Player target, @Nullable Text message, @Nullable MessageChannel original, Cause cause) {
        this.target = target;
        this.cause = cause;
        this.originalMessage = message;
        this.message = message;
        this.original = original;
        this.channel = original;
    }

    @Override
    public Player getTargetEntity() {
        return this.target;
    }

    @Override
    public Cause getCause() {
        return this.cause;
    }

    @Override
    public Optional<Text> getOriginalMessage() {
        return Optional.ofNullable(this.originalMessage);
    }

    @Override
    public Optional<Text> getMessage() {
        return Optional.ofNullable(this.message);
    }

    @Override
    public void setMessage(@Nullable Text message) {
        this.message = message;
    }

    @Override
    public MessageChannel getOriginalChannel() {
        return this.original;
    }

    @Override
    public MessageChannel getChannel() {
        return this.channel;
    }

    @Override
    public void setChannel(MessageChannel channel) {
        this.channel = Preconditions.checkNotNull(channel);
    }

    public static class From extends AFKEvents implements NucleusAFKEvent.ReturningFromAFK {

        public From(Player target, @Nullable Text message, @Nullable MessageChannel original, Cause cause) {
            super(target, message, original, cause);
        }
    }

    public static class To extends AFKEvents implements NucleusAFKEvent.GoingAFK {

        public To(Player target, @Nullable Text message, @Nullable MessageChannel original) {
            super(target, message, original);
        }

        public To(Player target, @Nullable Text message, @Nullable MessageChannel original, Cause cause) {
            super(target, message, original, cause);
        }
    }

    public static class Kick extends AFKEvents implements NucleusAFKEvent.Kick {

        private boolean cancelled = false;

        public Kick(Player target, Text message, MessageChannel original) {
            super(target, message, original);
        }

        public Kick(Player target, Text message, MessageChannel original, Cause cause) {
            super(target, message, original, cause);
        }

        @Override public boolean isCancelled() {
            return this.cancelled;
        }

        @Override public void setCancelled(boolean cancel) {
            this.cancelled = cancel;
        }
    }

    public static class Notify implements NucleusAFKEvent.NotifyCommand {

        private final Player target;
        private final Cause cause;
        @Nullable private final Text originalMessage;
        @Nullable private Text message;

        public Notify(Player target, @Nullable Text message, Cause cause) {
            this.target = target;
            this.originalMessage = message;
            this.message = message;
            this.cause = cause;
        }

        @Override public Cause getCause() {
            return this.cause;
        }

        @Override public Optional<Text> getOriginalMessage() {
            return Optional.ofNullable(this.originalMessage);
        }

        @Override public Optional<Text> getMessage() {
            return Optional.ofNullable(this.message);
        }

        @Override public Player getTargetEntity() {
            return this.target;
        }

        @Override public void setMessage(@Nullable Text message) {
            this.message = message;
        }
    }
}
