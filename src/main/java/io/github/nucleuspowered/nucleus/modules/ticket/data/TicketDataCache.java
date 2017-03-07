/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ticket.data;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Ticket;
import io.github.nucleuspowered.nucleus.api.query.QueryComparator;
import io.github.nucleuspowered.nucleus.api.query.QueryDataCache;
import io.github.nucleuspowered.nucleus.api.query.TicketQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class TicketDataCache {
    private TicketDataManager ticketDataManager;
    private QueryDataCache<Integer, TicketData> cachedTickets; //TODO Actually cache retrieved data and serve it.

    public TicketDataCache(TicketDataManager ticketDataManager) {
        this.ticketDataManager = ticketDataManager;
    }

    public CompletableFuture<Collection<Ticket>> lookupTicket(TicketQuery query) throws SQLException {
        CompletableFuture<Collection<Ticket>> future = new CompletableFuture<>();
        if (!ticketDataManager.getDataSource().isPresent()) {
            throw new SQLException("Retrieving Tickets from the database failed... the data source is not present.");
        }

        future.supplyAsync(() -> {
            List<Ticket> result = Lists.newArrayList();
            try (
                    Connection connection = ticketDataManager.getDataSource().get().getConnection();
                    PreparedStatement statement = query.constructQuery(connection)
            ) {
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    int id = resultSet.getInt("ID");
                    UUID owner = UUID.fromString(resultSet.getString("Owner"));
                    UUID assignee = resultSet.getString("Assignee") != null ? UUID.fromString(resultSet.getString("Assignee")) : null;
                    long creationDate = resultSet.getTimestamp("CreationDate").getTime();
                    long lastUpdateDate = resultSet.getTimestamp("LastUpdateDate").getTime();
                    boolean closed = resultSet.getBoolean("Closed");

                    lookupTicketMessagesByTicketID(id).thenAccept(message -> result.add(new TicketData(id, owner, assignee, creationDate, lastUpdateDate, message, closed)));
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                return future.completeExceptionally(ex);
            }

            return future.complete(result);
        }, ticketDataManager.getExecutor());

        return future;
    }

    public CompletableFuture<Optional<Ticket>> lookupTicketByID(int id) throws SQLException {
        return lookupTicket(TicketQuery.builder().filter(TicketQuery.Column.ID, QueryComparator.EQUALS, id).build())
                .thenApply(message -> Optional.ofNullable(message.isEmpty() ? null : message.iterator().next() /* An ID is unique, we know there will only be one so get the first. */));
    }

    public CompletableFuture<Collection<Ticket>> lookupTicketsByOwner(UUID uuid) throws SQLException {
        return lookupTicket(TicketQuery.builder().filter(TicketQuery.Column.OWNER, QueryComparator.EQUALS, uuid).build());
    }

    public CompletableFuture<Collection<Ticket>> lookupTicketsByAssignee(UUID uuid) throws SQLException {
        return lookupTicket(TicketQuery.builder().filter(TicketQuery.Column.ASSIGNEE, QueryComparator.EQUALS, uuid).build());
    }

    public CompletableFuture<Collection<Ticket>> lookupTicketsInCreationDateRange(Instant lowerDate, Instant higherDate) throws SQLException {
        return lookupTicket(TicketQuery.builder().filter(TicketQuery.Column.CREATION_DATE, QueryComparator.BETWEEN, lowerDate, higherDate).build());
    }

    public CompletableFuture<Collection<Ticket>> lookupTicketsInUpdateDateRange(Instant lowerDate, Instant higherDate) throws SQLException {
        return lookupTicket(TicketQuery.builder().filter(TicketQuery.Column.LAST_UPDATE_DATE, QueryComparator.BETWEEN, lowerDate, higherDate).build());
    }

    public CompletableFuture<Collection<Ticket>> lookupTicketsByStatus(boolean closed) throws SQLException {
        return lookupTicket(TicketQuery.builder().filter(TicketQuery.Column.STATUS, QueryComparator.EQUALS, closed).build());
    }

    public CompletableFuture<TreeMap<Long, String>> lookupTicketMessagesByTicketID(int id) throws SQLException {
        CompletableFuture<TreeMap<Long, String>> future = new CompletableFuture<>();
        TreeMap<Long, String> result = Maps.newTreeMap();
        String query = "SELECT * FROM Ticket_Messages WHERE TicketID = ?";

        if (!ticketDataManager.getDataSource().isPresent()) {
            throw new SQLException("Retrieving Tickets from the database failed... the data source is not present.");
        }

        future.supplyAsync(() -> {
            try (
                    Connection connection = ticketDataManager.getDataSource().get().getConnection();
                    PreparedStatement statement = connection.prepareStatement(query)
            ) {
                statement.setInt(1, id);

                ResultSet resultSet = statement.executeQuery(query);
                while (resultSet.next()) {
                    long date = resultSet.getLong("MessageDate");
                    String message = resultSet.getString("Message");

                    result.put(date, message);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                return future.completeExceptionally(ex);
            }

            return future.complete(result);
        }, ticketDataManager.getExecutor());

        return future;
    }
}
