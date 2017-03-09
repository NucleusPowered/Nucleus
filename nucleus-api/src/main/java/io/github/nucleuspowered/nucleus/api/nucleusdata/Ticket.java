/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.nucleusdata;

import java.time.Instant;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Information about a support ticket.
 */
public interface Ticket {
    /**
     * The unique Integer ID of this ticket
     *
     * @return The tickets ID.
     */
    int getId();

    /**
     * The {@link UUID} of the Player which created this ticket.
     *
     * @return The UUID of the Player.
     */
    UUID getOwner();

    /**
     * The {@link UUID} of the assigned user, or {@link Optional#empty()} if there is no assignee.
     *
     * @return The UUID of the assignee.
     */
    Optional<UUID> getAssignee();

    /**
     * When the ticket was created.
     *
     * @return The {@link Instant} the ticket was created.
     */
    Instant getCreationDate();

    /**
     * When the ticket was last updated.
     *
     * @return The {@link Instant} the ticket was last updated.
     */
    Instant getLastUpdateDate();

    /**
     * A Map containing all communicated messages in this ticket. A long derived from the {@link Instant} of when the
     * message was communicated is mapped to the message itself, the map is sorted in time order.
     *
     * @return A map representing all communicated messages.
     */
    TreeMap<Long, String> getMessages();

    /**
     * If the ticket is closed.
     *
     * @return <code>true</code> if so.
     */
    boolean isClosed();
}
