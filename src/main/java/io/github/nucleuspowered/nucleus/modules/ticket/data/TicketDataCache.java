/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ticket.data;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Ticket;
import org.apache.commons.lang3.tuple.Triple;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class TicketDataCache {
    private TicketDataManager ticketDataManager;
    private Map<Integer, TicketData> cachedTickets = Maps.newHashMap(); //TODO Actually cache retrieved data and serve it.

    public TicketDataCache(TicketDataManager ticketDataManager) {
        this.ticketDataManager = ticketDataManager;
    }

    public CompletableFuture<List<Ticket>> lookupTicket(List<Triple<String, String, String>> arguments) throws SQLException {
        CompletableFuture<List<Ticket>> future = new CompletableFuture<>();
        List<Ticket> result = Lists.newArrayList();
        //Create a selection query based on the list of Tri's provided. The first should be the column, the second should be the comparator and the third should be the value.
        String query = "SELECT * FROM Tickets";
        if (arguments.size() > 0) {
            query += " WHERE (";
            Iterator<Triple<String, String, String>> argumentIterator = arguments.iterator();
            while (argumentIterator.hasNext()) {
                Triple<String, String, String> argument = argumentIterator.next();
                query += argument.getLeft() + " " + argument.getMiddle() + " " + argument.getRight();
                if (argumentIterator.hasNext()) query += " AND ";
            }
        }
        query += ");";

        if (!ticketDataManager.getDataSource().isPresent()) throw new SQLException("Retrieving Tickets from the database failed... the data source is not present.");
        Connection connection = ticketDataManager.getDataSource().get().getConnection();
        Statement statement = connection.createStatement();

        ResultSet resultSet = statement.executeQuery(query);
        while (resultSet.next()) {
            int id = resultSet.getInt("ID");
            UUID owner = UUID.fromString(resultSet.getString("Owner"));
            UUID assignee = resultSet.getString("Assignee") != null ? UUID.fromString(resultSet.getString("Assignee")) : null;
            long creationDate = resultSet.getTimestamp("CreationDate").getTime();
            long lastUpdateDate = resultSet.getTimestamp("LastUpdateDate").getTime();
            boolean closed = resultSet.getBoolean("Closed");

            lookupTicketMessagesByTicketID(id).thenAccept(message -> result.add(new TicketData(id, owner, assignee, creationDate, lastUpdateDate, message, closed)));
        }

        statement.close();
        connection.close();

        future.complete(result);
        return future;
    }

    public CompletableFuture<Optional<Ticket>> lookupTicketByID(int id) throws SQLException {
        return lookupTicket(Lists.newArrayList(Triple.of("ID", "=", String.valueOf(id))))
                .thenApply(message -> Optional.ofNullable(message.get(0)));
    }

    public CompletableFuture<List<Ticket>> lookupTicketsByOwner(UUID uuid) throws SQLException {
        return lookupTicket(Lists.newArrayList(Triple.of("Owner", "=", uuid.toString())));
    }

    public CompletableFuture<List<Ticket>> lookupTicketsByAssignee(UUID uuid) throws SQLException {
        return lookupTicket(Lists.newArrayList(Triple.of("Assignee", "=", uuid.toString())));
    }

    public CompletableFuture<List<Ticket>> lookupTicketsByStatus(boolean closed) throws SQLException {
        return lookupTicket(Lists.newArrayList(Triple.of("Closed", "=", String.valueOf(closed))));
    }

    public CompletableFuture<TreeMap<Long, String>> lookupTicketMessagesByTicketID(int id) throws SQLException {
        CompletableFuture<TreeMap<Long, String>> future = new CompletableFuture<>();
        TreeMap<Long, String> result = Maps.newTreeMap();
        String query = "SELECT * FROM Ticket_Messages WHERE TicketID = ?";

        if (!ticketDataManager.getDataSource().isPresent()) throw new SQLException("Retrieving Tickets from the database failed... the data source is not present.");
        Connection connection = ticketDataManager.getDataSource().get().getConnection();
        PreparedStatement statement = connection.prepareStatement(query);

        statement.setInt(1, id);

        ResultSet resultSet = statement.executeQuery(query);
        while (resultSet.next()) {
            long date = resultSet.getLong("MessageDate");
            String message = resultSet.getString("Message");

            result.put(date, message);
        }

        statement.close();
        connection.close();

        future.complete(result);
        return future;
    }
}
