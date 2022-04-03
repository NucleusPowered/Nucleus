/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.module.message.target;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

/**
 * The target of a private message.
 */
public interface MessageTarget {

    /**
     * Gets an {@link Audience} that represents this target, if any.
     *
     * @return The {@link Audience}
     */
    Optional<Audience> getRepresentedAudience();

    /**
     * The name of this target.
     *
     * @return The name.
     */
    default String getName() {
        return PlainTextComponentSerializer.plainText().serialize(this.getDisplayName());
    }

    /**
     * The display name of this target.
     *
     * @return The display name
     */
    Component getDisplayName();

    /**
     * Called when this target receives a private message.
     *
     * @param userID The {@link MessageTarget} of the user who sent the message.
     * @param message The message.
     */
    void receiveMessageFrom(final MessageTarget userID, final Component message);

    /**
     * Gets the reply target for this target.
     *
     * @return The reply target.
     */
    Optional<? extends MessageTarget> replyTarget();

    /**
     * Sets the reply target for this target.
     *
     * @param messageTarget The reply target.
     */
    void setReplyTarget(@Nullable MessageTarget messageTarget);

    /**
     * Gets whether the target can currently receive messages.
     *
     * @return true if so
     */
    boolean isAvailableForMessages();

    /**
     * Gets whether messages sent by this target will be sent to a receiver
     * regardless of whether they have toggled receiving messages off.
     *
     * @return If this target can bypass.
     */
    boolean canBypassMessageToggle();

}
