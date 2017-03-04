/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.service;

import io.github.nucleuspowered.nucleus.api.nucleusdata.Ticket;
import org.apache.commons.lang3.tuple.Triple;
import org.spongepowered.api.entity.living.player.User;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface NucleusTicketService {

    /**
     * Gets the ticket with the specified ID.
     *
     * @param id The ID of the ticket to get.
     * @return The {@link Ticket} if present.
     */
    CompletableFuture<Optional<Ticket>> getTicketWithID(int id);

    /**
     * Gets all tickets (open and closed) owned by a {@link User}.
     *
     * @param user The {@link User} that has created the tickets.
     * @return The {@link Ticket}s.
     */
    CompletableFuture<List<Ticket>> getTicketsForOwner(User user);

    /**
     * Gets all tickets (open and closed) assigned to a {@link User}.
     *
     * @param user The {@link User} assigned to the tickets.
     * @return The {@link Ticket}s.
     */
    CompletableFuture<List<Ticket>> getTicketsForAssignee(User user);

    /**
     * Gets all tickets by their status, if they are open or closed.
     *
     * @param closed The status of the tickets to get, <code>true</code> if closed.
     * @return The {@link Ticket}s.
     */
    CompletableFuture<List<Ticket>> getTicketsByStatus(boolean closed);

    /**
     * Gets all tickets with the specified argument, each Tri represents an argument, the first should be the column
     * identifier, the second should be the matching operator (=, >, <, >=, <=, etc.), SQL matches such as BETWEEN can
     * also be used., the third should be the value to match.
     *
     * @param arguments The arguments to fetch tickets
     * @return The {@link Ticket}s which matched the query.
     */
    CompletableFuture<List<Ticket>> getTicketsByArguments(List<Triple<String, String, String>> arguments);

    /**
     * Creates a ticket for the specified user.
     *
     * @param creator        The user that created the ticket.
     * @param initialMessage The initial message for the ticket.
     * @return <code>true</code> if successful.
     */
    boolean createTicket(User creator, String initialMessage);

    /**
     * Adds a message to the ticket with the specified ID.
     *
     * @param id      The ID of the ticket to add the message to.
     * @param message The message to add.
     * @return <code>true</code> if successful.
     */
    boolean addMessageByTicketID(int id, String message);
}
