/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ticket.data;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.api.filter.FilterComparator;
import io.github.nucleuspowered.nucleus.api.filter.NucleusTicketFilter;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Ticket;
import io.github.nucleuspowered.nucleus.modules.ticket.filter.sql.TicketQuery;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.sql.SqlService;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;

public class TicketDataManager {
    @Inject private NucleusPlugin nucleus;

    private final String DATABASE_URL = "jdbc:h2:./nucleus/TicketData";

    private final String[] CREATE_TABLE_QUERIES = {
            //Create table to store basic ticket data
            "CREATE TABLE IF NOT EXISTS Tickets(" +
                    "ID INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, " +
                    "Owner CHAR(36) NOT NULL, " +
                    "Assignee CHAR(36), " +
                    "CreationDate TIMESTAMP NOT NULL, " +
                    "LastUpdateDate TIMESTAMP NOT NULL, " +
                    "Closed BOOLEAN NOT NULL);",
            //Create indexes for Tickets table by owner, assignee and closed.
            "CREATE INDEX IF NOT EXISTS owner ON Tickets(Owner);",
            "CREATE INDEX IF NOT EXISTS assignee ON Tickets(Assignee);",
            "CREATE INDEX IF NOT EXISTS closed ON Tickets(Closed);",

            //Create table to store actions conducted on Tickets
            "CREATE TABLE IF NOT EXISTS Ticket_Actions(" +
                    "ID INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, " +
                    "TicketID INTEGER NOT NULL, " +
                    "ACTOR CHAR(36) NOT NULL, " +
                    "ActionDate TIMESTAMP NOT NULL, " +
                    "ActionMessage TEXT NOT NULL);",
            //Create index for Ticket_Audits table by TicketID.
            "CREATE INDEX IF NOT EXISTS ticketid ON Ticket_Actions(TicketID);",
            "CREATE INDEX IF NOT EXISTS actiondate ON Ticket_Actions(ActionDate);",

            //Create table to store ticket messages
            "CREATE TABLE IF NOT EXISTS Ticket_Messages(" +
                    "ID INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, " +
                    "TicketID INTEGER NOT NULL, " +
                    "MessageDate TIMESTAMP NOT NULL, " +
                    "Message TEXT NOT NULL, " +
                    "CONSTRAINT FK_ID FOREIGN KEY(TicketID) REFERENCES Tickets(ID) ON DELETE CASCADE);",
            //Create index for Ticket_Messages table by TicketID.
            "CREATE INDEX IF NOT EXISTS ticketid ON Ticket_Messages(TicketID);"
    };

    public static final String CREATE_TICKET_QUERY = "INSERT INTO Tickets(Owner, Assignee, CreationDate, LastUpdateDate, Closed) VALUES(?, ?, ?, ?, ?);";
    public static final String UPDATE_TICKET_QUERY = "UPDATE Tickets SET Owner = ?, Assignee = ?, CreationDate = ?, LastUpdateDate = ?, Closed = ? WHERE ID = ?;";
    public static final String DELETE_TICKET_QUERY = "DELETE FROM Tickets WHERE ID = ?;";

    public static final String CREATE_TICKET_MESSAGE_QUERY = "INSERT INTO Ticket_Messages(TicketID, MessageDate, Message) VALUES(?, ?, ?);";
    public static final String UPDATE_TICKET_MESSAGE_QUERY = "UPDATE Ticket_Messages SET Message = ? WHERE(TicketID = ? AND MessageDate = ?);";
    public static final String DELETE_TICKET_MESSAGE_QUERY = "DELETE FROM Ticket_Messages WHERE(TicketID = ? AND MessageDate = ? AND Message = ?);";

    public static final String CREATE_TICKET_ACTION_QUERY = "INSERT INTO Ticket_Actions(TicketID, ActionDate, ActionMessage) VALUES(?, ?, ?);";

    public Optional<DataSource> getDataSource() {
        SqlService sqlService = Sponge.getServiceManager().provide(SqlService.class).get();

        try {
            return Optional.of(sqlService.getDataSource(DATABASE_URL));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return Optional.empty();
    }

    public boolean createTables() throws SQLException {
        if (!getDataSource().isPresent()) {
            throw new SQLException("Creating the tables in the database failed... the data source is not present.");
        }

        try (
                Connection connection = getDataSource().get().getConnection();
                Statement statement = connection.createStatement()
        ) {
            //Batch all table creation queries and then execute at oncce.
            for (String query : CREATE_TABLE_QUERIES) {
                statement.addBatch(query);
            }

            statement.executeBatch();
        }

        return true;
    }

    public boolean createTicket(TicketData ticket) throws SQLException {
        if (!getDataSource().isPresent()) {
            throw new SQLException("Creating the Ticket in the database failed... the data source is not present.");
        }

        try (
                Connection connection = getDataSource().get().getConnection();
                PreparedStatement statement = connection.prepareStatement(CREATE_TICKET_QUERY, Statement.RETURN_GENERATED_KEYS)
        ) {
            statement.setString(1, ticket.getOwner().toString());
            statement.setString(2, ticket.getAssignee().isPresent() ? ticket.getAssignee().get().toString() : null);
            statement.setTimestamp(3, Timestamp.from(ticket.getCreationDate()));
            statement.setTimestamp(4, Timestamp.from(ticket.getLastUpdateDate()));
            statement.setBoolean(5, ticket.isClosed());

            //Check if anything was affected, if it wasn't throw an exception.
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Creating the Ticket in the database failed... no rows were affected.");
            }

            //Get the Ticket ID and set it in the ticket which was passed, if no ID could be obtained throw an exception.
            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                ticket.setId(generatedKeys.getInt(1));
            } else {
                throw new SQLException("Creating the Ticket in the database failed... the ID could not be obtained");
            }
        }

        //Create all ticket messages in database also.
        ticket.getMessages().entrySet().forEach(message -> {
            try {
                createTicketMessage(ticket, Instant.ofEpochMilli(message.getKey()), message.getValue());
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        return true;
    }

    public Collection<Ticket> lookupTicket(Set<NucleusTicketFilter> filters) throws SQLException {
        if (!getDataSource().isPresent()) {
            throw new SQLException("Retrieving Tickets from the database failed... the data source is not present.");
        }

        Collection<Ticket> result = Lists.newArrayList();
        try (
                Connection connection = getDataSource().get().getConnection();
                PreparedStatement statement = TicketQuery.fromFilters(filters).constructQuery(connection)
        ) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("ID");
                UUID owner = UUID.fromString(resultSet.getString("Owner"));
                UUID assignee = resultSet.getString("Assignee") != null ? UUID.fromString(resultSet.getString("Assignee")) : null;
                long creationDate = Timestamp.valueOf(resultSet.getString("CreationDate")).getTime();
                long lastUpdateDate = Timestamp.valueOf(resultSet.getString("LastUpdateDate")).getTime();
                boolean closed = resultSet.getBoolean("Closed");
                TreeMap<Long, String> messages = lookupTicketMessagesByTicketID(id);

                result.add(new TicketData(id, owner, assignee, creationDate, lastUpdateDate, messages, closed));
            }
        }
        return result;
    }

    public Ticket lookupTicketByID(int id) throws SQLException {
        Collection<Ticket> tickets = lookupTicket(NucleusTicketFilter.builder().filter(NucleusTicketFilter.Property.ID, FilterComparator.EQUALS, id).build());
        if (tickets.isEmpty()) {
            return null;
        } else {
            return tickets.iterator().next(); //There can only be one ticket, ID's are unique.
        }
    }

    public boolean updateTicket(Ticket ticket) throws SQLException {
        return updateTicket(ticket, false);
    }

    public boolean updateTicket(Ticket ticket, boolean updateMessages) throws SQLException {
        if (!getDataSource().isPresent()) {
            throw new SQLException("Updating the Ticket in the database failed... the data source is not present.");
        }

        try (
                Connection connection = getDataSource().get().getConnection();
                PreparedStatement statement = connection.prepareStatement(UPDATE_TICKET_QUERY)
        ) {
            statement.setString(1, ticket.getOwner().toString());
            statement.setString(2, ticket.getAssignee().isPresent() ? ticket.getAssignee().get().toString() : null);
            statement.setTimestamp(3, Timestamp.from(ticket.getCreationDate()));
            statement.setTimestamp(4, Timestamp.from(ticket.getLastUpdateDate()));
            statement.setBoolean(5, ticket.isClosed());
            statement.setInt(6, ticket.getId());

            //Check if anything was affected, if it wasn't send a warning.
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 0) {
                nucleus.getLogger().warn("Updating the Ticket with ticket id " + ticket.getId() + " in the database failed... no rows were affected."); //There may be nothing to update or something could have gone bad.
            }
        }

        if (updateMessages) {
            updateTicketMessages(ticket);
        }

        return true;
    }

    public boolean deleteTicket(int id) throws SQLException {
        if (!getDataSource().isPresent()) {
            throw new SQLException("Deleting the Ticket in the database failed... the data source is not present.");
        }

        try (
                Connection connection = getDataSource().get().getConnection();
                PreparedStatement statement = connection.prepareStatement(DELETE_TICKET_QUERY)
        ) {
            statement.setInt(1, id);

            //Check if anything was affected, if it wasn't throw an exception.
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Deleting the Ticket in the database failed... no rows were affected.");
            }
        }

        return true;
    }

    public boolean deleteTicket(Ticket ticket) throws SQLException {
        return deleteTicket(ticket.getId());
    }

    public boolean createTicketMessage(Ticket ticket, Instant creationDate, String message) throws SQLException {
        if (!getDataSource().isPresent()) {
            throw new SQLException("Creating the Ticket Message in the database failed... the data source is not present.");
        }

        try (
                Connection connection = getDataSource().get().getConnection();
                PreparedStatement statement = connection.prepareStatement(CREATE_TICKET_MESSAGE_QUERY)
        ) {
            statement.setInt(1, ticket.getId());
            statement.setTimestamp(2, Timestamp.from(creationDate));
            statement.setString(3, message);

            //Check if anything was affected, if it wasn't throw an exception.
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Creating the Ticket Message in the database failed... no rows were affected.");
            }
        }

        return true;
    }

    public TreeMap<Long, String> lookupTicketMessagesByTicketID(int id) throws SQLException {
        if (!getDataSource().isPresent()) {
            throw new SQLException("Retrieving Tickets from the database failed... the data source is not present.");
        }

        TreeMap<Long, String> result = Maps.newTreeMap();
        String query = "SELECT * FROM Ticket_Messages WHERE TicketID = ?";

        try (
                Connection connection = getDataSource().get().getConnection();
                PreparedStatement statement = connection.prepareStatement(query)
        ) {
            statement.setInt(1, id);

            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                long date = resultSet.getTimestamp("MessageDate").getTime();
                String message = resultSet.getString("Message");

                result.put(date, message);
            }
        }

        return result;
    }

    public boolean updateTicketMessage(Ticket ticket, Instant creationDate, String newMessage) throws SQLException {
        if (!getDataSource().isPresent()) {
            throw new SQLException("Updating the Ticket Message in the database failed... the data source is not present.");
        }

        try (
                Connection connection = getDataSource().get().getConnection();
                PreparedStatement statement = connection.prepareStatement(UPDATE_TICKET_MESSAGE_QUERY)
        ) {
            statement.setString(1, newMessage);
            statement.setInt(2, ticket.getId());
            statement.setTimestamp(3, Timestamp.from(creationDate));

            //Check if anything was affected, if it wasn't send a warning.
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 0) {
                nucleus.getLogger().warn("Updating a Ticket Message for ticket id " + ticket.getId() + " in the database failed... no rows were affected."); //There may be nothing to update or something could have gone bad.
            }
        }

        return true;
    }

    public boolean updateTicketMessages(Ticket ticket) throws SQLException {
        if (!getDataSource().isPresent()) {
            throw new SQLException("Updating the Ticket Message in the database failed... the data source is not present.");
        }

        try (
                Connection connection = getDataSource().get().getConnection();
                PreparedStatement statement = connection.prepareStatement(UPDATE_TICKET_MESSAGE_QUERY)
        ) {
            ticket.getMessages().entrySet().forEach(entry -> {
                try {
                    statement.setString(1, entry.getValue());
                    statement.setInt(2, ticket.getId());
                    statement.setTimestamp(3, Timestamp.from(Instant.ofEpochMilli(entry.getKey())));
                    statement.addBatch();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            });

            //Check if anything was affected, if it wasn't send a warning.
            int rowsAffected = IntStream.of(statement.executeBatch()).sum();
            if (rowsAffected == 0) {
                nucleus.getLogger().warn("Updating the Ticket Messages for ticket id " + ticket.getId() + " in the database failed... no rows were affected."); //There may be nothing to update or something could have gone bad.
            }
        }

        return true;
    }

    public boolean deleteTicketMessage(Ticket ticket, Instant creationDate, String message) throws SQLException {
        if (!getDataSource().isPresent()) {
            throw new SQLException("Deleting the Ticket Message in the database failed... the data source is not present.");
        }

        try (
                Connection connection = getDataSource().get().getConnection();
                PreparedStatement statement = connection.prepareStatement(DELETE_TICKET_MESSAGE_QUERY)
        ) {
            statement.setInt(1, ticket.getId());
            statement.setTimestamp(2, Timestamp.from(creationDate));
            statement.setString(3, message);

            //Check if anything was affected, if it wasn't throw an exception.
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Deleting the Ticket in the database failed... no rows were affected.");
            }
        }

        return true;
    }

    public boolean createTicketAction(Ticket ticket, Instant date, String action) throws SQLException {
        if (!getDataSource().isPresent()) {
            throw new SQLException("Creating the Ticket Action in the database failed... the data source is not present.");
        }

        try (
                Connection connection = getDataSource().get().getConnection();
                PreparedStatement statement = connection.prepareStatement(CREATE_TICKET_ACTION_QUERY)
        ) {
            statement.setInt(1, ticket.getId());
            statement.setTimestamp(2, Timestamp.from(date));
            statement.setString(3, action);

            //Check if anything was affected, if it wasn't throw an exception.
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Creating the Ticket Action in the database failed... no rows were affected.");
            }
        }

        return true;
    }

    //TODO Create Retrieve query for Ticket Actions
}
