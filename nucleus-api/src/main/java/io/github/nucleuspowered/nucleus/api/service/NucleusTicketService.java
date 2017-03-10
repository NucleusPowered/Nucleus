/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.service;

import io.github.nucleuspowered.nucleus.api.nucleusdata.Ticket;
import io.github.nucleuspowered.nucleus.api.query.NucleusTicketQuery;
import org.spongepowered.api.entity.living.player.User;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface NucleusTicketService {

    /**
     * Looks up all Tickets that fulfill the provided query criteria.
     *
     * @param query The query to use when loooking up tickets.
     * @return A collection of all {@link Ticket}s which fulfill the provided query.
     */
    CompletableFuture<Collection<Ticket>> lookupTicket(NucleusTicketQuery query);

    /**
     * Looks up all Tickets that were created by the user with the provided {@link UUID}.
     *
     * @param uuid The uuid of the creator to lookup tickets for.
     * @return A collection of all {@link Ticket}s created by the provided {@link UUID}.
     */
    CompletableFuture<Collection<Ticket>> lookupTicketsByOwner(UUID uuid);

    /**
     * Looks up all Tickets that are assigned to a user with the provided {@link UUID}.
     *
     * @param uuid The uuid of the assignee to lookup tickets for.
     * @return A collection of all {@link Ticket}s assigned to the provided {@link UUID}.
     */
    public CompletableFuture<Collection<Ticket>> lookupTicketsByAssignee(UUID uuid);

    /**
     * Looks up all Tickets that were created in the specified range of dates.
     *
     * @param lowerDate  The lower date to compare by.
     * @param higherDate The higher date to compare by.
     * @return A collection of all {@link Ticket}s created in the provided range of dates.
     */
    CompletableFuture<Collection<Ticket>> lookupTicketsInCreationDateRange(Instant lowerDate, Instant higherDate);

    /**
     * Looks up all Tickets that were updated in the specified range of dates.
     *
     * @param lowerDate  The lower date to compare by.
     * @param higherDate The higher date to compare by.
     * @return A collection of all {@link Ticket}s updated in the provided range of dates.
     */
    CompletableFuture<Collection<Ticket>> lookupTicketsInUpdateDateRange(Instant lowerDate, Instant higherDate);

    /**
     * Looks up all Tickets by their status (If they are open or closed).
     *
     * @param closed The status of the Ticket to lookup, <code>true</code> if all closed Tickets need to be retrieved,
     *               else <code>false</code>.
     * @return A collection of all {@link Ticket}s that are either open or closed.
     */
    CompletableFuture<Collection<Ticket>> lookupTicketsByStatus(boolean closed);

    /**
     * Looks up a Ticket by it's id.
     *
     * @param id       The id of the Ticket to lookup.
     * @param useCache If the cache should be used to lookup the Ticket first.
     * @return The {@link Ticket} if found.
     */
    CompletableFuture<Ticket> lookupTicketById(int id, boolean useCache);

    /**
     * Looks up a Ticket by it's id, the cache is used.
     *
     * @param id The id of the Ticket to lookup.
     * @return The {@link Ticket} if found.
     */
    CompletableFuture<Ticket> lookupTicketById(int id);

    /**
     * Creates a Ticket with an initial message, it is saved in the database and cached.
     *
     * @param creator        The {@link User} which has created this ticket.
     * @param initialMessage The initial message the Ticket should be created with.
     * @return <code>true</code> if successful.
     */
    CompletableFuture<Boolean> createTicket(User creator, String initialMessage);

    /**
     * Updates a Ticket in the database, manually, with its new properties.
     *
     * @param updatedTicket  The ticket object with the updated properties.
     * @param updateMessages If the messages should also be updated in the database.
     * @return <code>true</code> if successful.
     */
    CompletableFuture<Boolean> updateTicket(Ticket updatedTicket, boolean updateMessages);

    /**
     * Deletes the ticket with the provided id.
     *
     * @param id The id of the ticket to delete.
     * @return <code>true</code> if successful.
     */
    CompletableFuture<Boolean> deleteTicket(int id);

    /**
     * Adds a message to the Ticket with the specified id. The creation date is set to the {@link Instant} at which this
     * method is called.
     *
     * @param id      The id of the Ticket to add the message to.
     * @param message The message to add.
     * @return <code>true</code> if successful.
     */
    CompletableFuture<Boolean> addMessageToTicket(int id, String message);

    /**
     * Edits the Ticket message in the Ticket with the supplied id. The ticket message is determined by its creation
     * date.
     *
     * @param id           The id of the Ticket which contains the message to edit.
     * @param creationDate The creation date of the message to edit.
     * @param newMessage   The new message to replace the old one.
     * @return <code>true</code> if successful.
     */
    CompletableFuture<Boolean> editMessageInTicket(int id, Instant creationDate, String newMessage);

    /**
     * Deletes the Ticket message in the Ticket with the supplied id. The ticket message is determined by the
     * provided creation date and message.
     *
     * @param id           The id of the Ticket which contains the message to delete.
     * @param creationDate The creation date of the message to delete.
     * @param message      The message of the message to delete.
     * @return <code>true</code> if successful.
     */
    CompletableFuture<Boolean> deleteMessageInTicket(int id, Instant creationDate, String message);

    /**
     * Invalidates the Ticket cache entry for the provided ID.
     *
     * @param id The id of the Ticket to invalidate the cache entry for.
     */
    void invalidateCacheEntry(int id);

    /**
     * Invalidates all Ticket entries in the cache with the supplied ids.
     *
     * @param ids The ids of the Tickets to invalidate in the cache.
     */
    void invalidateCacheEntries(Iterable<Integer> ids);

    /**
     * Invalidates all cached Ticket entries.
     */
    void invalidateCache();
}
