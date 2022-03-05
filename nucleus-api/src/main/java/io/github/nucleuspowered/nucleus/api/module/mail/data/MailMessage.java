/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.module.mail.data;

import org.spongepowered.api.data.persistence.DataSerializable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a mail message.
 */
public interface MailMessage extends DataSerializable {

    /**
     * The message that was sent.
     *
     * @return The message.
     */
    String getMessage();

    /**
     * The time the message was sent.
     *
     * @return The {@link Instant}
     */
    Instant getDate();

    /**
     * The {@link UUID} of the sender of the message, or
     * {@link Optional#empty()} if it wasn't a player.
     *
     * @return The sender.
     */
    Optional<UUID> getSender();
}
