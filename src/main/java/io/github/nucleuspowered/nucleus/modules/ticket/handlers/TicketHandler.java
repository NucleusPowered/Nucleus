/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ticket.handlers;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.api.filter.FilterComparator;
import io.github.nucleuspowered.nucleus.api.filter.NucleusTicketFilter;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Ticket;
import io.github.nucleuspowered.nucleus.api.service.NucleusTicketService;
import io.github.nucleuspowered.nucleus.modules.ticket.data.TicketData;
import io.github.nucleuspowered.nucleus.modules.ticket.data.TicketDataManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class TicketHandler implements NucleusTicketService {
    private final NucleusPlugin nucleus;
    private final TicketDataManager ticketDataManager;
    private final ExecutorService executor;
    private final AsyncLoadingCache<Integer, Ticket> cache;

    public TicketHandler(NucleusPlugin nucleus, TicketDataManager ticketDataManager) {
        this.nucleus = nucleus;
        this.ticketDataManager = ticketDataManager;

        this.executor = Sponge.getScheduler().createAsyncExecutor(nucleus);
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(5, TimeUnit.SECONDS)
                .executor(executor)
                .removalListener((RemovalListener<Integer, Ticket>) (id, ticket, removalCause) -> {
                    if (!ticket.isDeleted()) { //We only want to update the ticket in the database if it hasn't been removed from the cache because it was deleted.
                        this.updateTicket(ticket, true, false);
                    }
                })
                .buildAsync(id -> this.lookupTicketById(id, false).get());
    }

    @Override
    public CompletableFuture<Collection<Ticket>> lookupTicket(Set<NucleusTicketFilter> filters) {
        CompletableFuture<Collection<Ticket>> future = new CompletableFuture<>();

        executor.execute(() -> {
            try {
                Collection<Ticket> ticket = ticketDataManager.lookupTicket(filters);
                future.complete(ticket);
            } catch (SQLException ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<Collection<Ticket>> lookupTicketsByOwner(UUID uuid) {
        return lookupTicket(NucleusTicketFilter.builder().filter(NucleusTicketFilter.Property.OWNER, FilterComparator.EQUALS, uuid).build());
    }

    @Override
    public CompletableFuture<Collection<Ticket>> lookupTicketsByAssignee(UUID uuid) {
        return lookupTicket(NucleusTicketFilter.builder().filter(NucleusTicketFilter.Property.ASSIGNEE, FilterComparator.EQUALS, uuid).build());
    }

    @Override
    public CompletableFuture<Collection<Ticket>> lookupTicketsInCreationDateRange(Instant lowerDate, Instant higherDate) {
        return lookupTicket(NucleusTicketFilter.builder().filter(NucleusTicketFilter.Property.CREATION_DATE, FilterComparator.BETWEEN, lowerDate, higherDate).build());
    }

    @Override
    public CompletableFuture<Collection<Ticket>> lookupTicketsInUpdateDateRange(Instant lowerDate, Instant higherDate) {
        return lookupTicket(NucleusTicketFilter.builder().filter(NucleusTicketFilter.Property.LAST_UPDATE_DATE, FilterComparator.BETWEEN, lowerDate, higherDate).build());
    }

    @Override
    public CompletableFuture<Collection<Ticket>> lookupTicketsByStatus(boolean closed) {
        return lookupTicket(NucleusTicketFilter.builder().filter(NucleusTicketFilter.Property.STATUS, FilterComparator.EQUALS, closed).build());
    }

    @Override
    public CompletableFuture<Ticket> lookupTicketById(int id, boolean useCache) {
        if (useCache) {
            return cache.get(id);
        }

        CompletableFuture<Ticket> future = new CompletableFuture<>();

        executor.execute(() -> {
            try {
                Ticket ticket = ticketDataManager.lookupTicketByID(id);
                future.complete(ticket);
            } catch (SQLException ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<Ticket> lookupTicketById(int id) {
        return lookupTicketById(id, true);
    }

    @Override
    public CompletableFuture<Boolean> createTicket(User creator, String initialMessage, UUID assignee) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        TicketData ticketData = new TicketData(creator.getUniqueId(), assignee, initialMessage);

        //Create the ticket in the database.
        executor.execute(() -> {
            try {
                future.complete(ticketDataManager.createTicket(ticketData));
            } catch (SQLException ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        //After the future is computed put it in the cache. We have to wait until it is computed as the ID is not set before then.
        future.thenAcceptAsync(success -> {
            if (!success) {
                return;
            }

            cache.put(ticketData.getId(), CompletableFuture.completedFuture(ticketData));
        }, executor);

        return future;
    }

    @Override
    public CompletableFuture<Boolean> createTicket(User creator, String initialMessage) {
        return createTicket(creator, initialMessage, null);
    }

    @Override
    public CompletableFuture<Boolean> updateTicket(Ticket updatedTicket, boolean updateMessages, boolean reCache) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        //Update the ticket in the database.
        executor.execute(() -> {
            try {
                future.complete(ticketDataManager.updateTicket(updatedTicket, updateMessages));
            } catch (SQLException ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        if (reCache) {
            //After the future is computed put it in the cache. We have to wait until it is computed as if it fails data would be lost on a reboot.
            future.thenAcceptAsync(success -> {
                if (!success) {
                    return;
                }

                cache.put(updatedTicket.getId(), CompletableFuture.completedFuture(updatedTicket));
            }, executor);
        }

        return future;
    }

    @Override
    public CompletableFuture<Boolean> deleteTicket(int id) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        //Delete the ticket in the database.
        executor.execute(() -> {
            try {
                future.complete(ticketDataManager.deleteTicket(id));
            } catch (SQLException ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        //After the future is computed remove it from the cache. We have to wait until it is computed as if it fails data would be back after a reboot.
        future.thenAcceptBothAsync(lookupTicketById(id), (success, ticket) -> {
            if (!success) {
                return;
            }

            if (ticket != null) { //If the ticket still exists, set its deleted status to true.
                ticket.setDeleted(true);
            }

            invalidateCacheEntry(id);
        }, executor);

        return future;
    }

    @Override
    public CompletableFuture<Boolean> addMessageToTicket(int id, String message) {
        final Instant creationDate = Instant.now();

        //Lookup the ticket, and then try to add the message.
        CompletableFuture<Boolean> future = lookupTicketById(id)
                .thenApplyAsync(ticket -> {
                    if (ticket == null) {
                        return false;
                    }

                    try {
                        boolean success = ticketDataManager.createTicketMessage(ticket, creationDate, message);
                        if (success) {
                            ticket.getMessages().put(creationDate.toEpochMilli(), message);
                        }

                        return success;
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        return false;
                    }
                }, executor);

        return future;
    }

    @Override
    public CompletableFuture<Boolean> editMessageInTicket(int id, Instant creationDate, String newMessage) {
        //Lookup the ticket, and then try to edit the message.
        CompletableFuture<Boolean> future = lookupTicketById(id)
                .thenApplyAsync(ticket -> {
                    if (ticket == null) {
                        return false;
                    }

                    try {
                        boolean success = ticketDataManager.updateTicketMessage(ticket, creationDate, newMessage);
                        if (success) {
                            ticket.getMessages().put(creationDate.toEpochMilli(), newMessage);
                        }

                        return success;
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        return false;
                    }
                }, executor);

        return future;
    }

    @Override
    public CompletableFuture<Boolean> deleteMessageInTicket(int id, Instant creationDate, String message) {
        //Lookup the ticket, and then try to delete the message.
        CompletableFuture<Boolean> future = lookupTicketById(id)
                .thenApplyAsync(ticket -> {
                    if (ticket == null) {
                        return false;
                    }

                    try {
                        boolean success = ticketDataManager.deleteTicketMessage(ticket, creationDate, message);
                        if (success) {
                            ticket.getMessages().remove(creationDate.toEpochMilli());
                        }

                        return success;
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        return false;
                    }
                }, executor);

        return future;
    }

    @Override
    public CompletableFuture<Boolean> setTicketStatus(int id, boolean closed) {
        CompletableFuture<Boolean> future = lookupTicketById(id)
                .thenApplyAsync(ticket -> {
                    if (ticket == null) {
                        return false;
                    }

                    TicketData ticketData = (TicketData) ticket;
                    ticketData.setClosed(closed);

                    try {
                        return updateTicket(ticketData, false, true).get();
                    } catch (InterruptedException | ExecutionException ex) {
                        ex.printStackTrace();
                    }

                    return false;
                });

        return future;
    }

    @Override
    public void invalidateCacheEntry(int id) {
        cache.synchronous().invalidate(id);
    }

    @Override
    public void invalidateCacheEntries(Iterable<Integer> ids) {
        cache.synchronous().invalidateAll(ids);
    }

    @Override
    public void invalidateCache() {
        cache.synchronous().invalidateAll();
    }
}
