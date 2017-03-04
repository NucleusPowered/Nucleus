/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ticket.data;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Ticket;
import io.github.nucleuspowered.nucleus.modules.ticket.config.TicketConfigAdapter;
import org.apache.commons.lang3.tuple.Triple;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.sql.SqlService;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class TicketDataManager {
    public static String DATABASE_URL = "jdbc:h2:./nucleus/TicketData";

    public static String[] CREATE_TABLE_QUERIES = {
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

    public static String CREATE_TICKET_QUERY = "INSERT INTO Tickets(Owner, Assignee, CreationDate, LastUpdateDate, Closed) VALUES(?, ?, ?, ?, ?);";
    public static String UPDATE_TICKET_QUERY = "UPDATE Tickets SET Owner = ?, Assignee = ?, CreationDate = ?, LastUpdateDate = ?, Closed = ? WHERE ID = ?;";
    public static String DELETE_TICKET_QUERY = "DELETE FROM Tickets WHERE ID = ?;";

    public static String CREATE_TICKET_MESSAGE_QUERY = "INSERT INTO Ticket_Messages(TicketID, MessageDate, Message) VALUES(?, ?, ?);";
    public static String UPDATE_TICKET_MESSAGE_QUERY = "UPDATE Ticket_Messages SET Message = ? WHERE(TicketID = ? AND MessageDate = ?);";
    public static String DELETE_TICKET_MESSAGE_QUERY = "DELETE FROM Ticket_Messages WHERE(TicketID = ? AND MessageDate = ? AND Message = ?);";

    private NucleusPlugin nucleus;
    @Inject private TicketConfigAdapter tca;
    private ExecutorService executor;
    private TicketDataCache cache;

    @Inject
    public TicketDataManager(NucleusPlugin nucleus) {
        this.nucleus = nucleus;
        this.executor = Sponge.getScheduler().createAsyncExecutor(nucleus);
        this.cache = new TicketDataCache(this);
    }

    public Optional<DataSource> getDataSource() {
        SqlService sqlService = Sponge.getServiceManager().provide(SqlService.class).get();
        try {
            return Optional.of(sqlService.getDataSource(DATABASE_URL));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return Optional.empty();
    }

    public void createTables() {
        try {
            if (!getDataSource().isPresent()) return;
            Connection connection = getDataSource().get().getConnection();

            Statement statement = connection.createStatement();
            for (String query : CREATE_TABLE_QUERIES) {
                statement.addBatch(query);
            }
            statement.executeBatch();

            statement.close();
            connection.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void createTicket(TicketData ticket) throws SQLException {
        executor.execute(() -> {
            try {
                if (!getDataSource().isPresent()) throw new SQLException("Creating the Ticket in the database failed... the data source is not present.");
                Connection connection = getDataSource().get().getConnection();
                PreparedStatement statement = connection.prepareStatement(CREATE_TICKET_QUERY, Statement.RETURN_GENERATED_KEYS);

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

                statement.close();
                connection.close();

                //Create all ticket messages in database also.
                ticket.getMessages().entrySet().forEach(message -> {
                    try {
                        createTicketMessage(ticket, Instant.ofEpochMilli(message.getKey()), message.getValue());
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                });
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    public CompletableFuture<List<Ticket>> retrieveTickets(ArrayList<Triple<String, String, String>> arguments) throws SQLException {
        return cache.lookupTicket(arguments);
    }

    public void updateTicket(Ticket ticket) throws SQLException {
        executor.execute(() -> {
            try {
                if (!getDataSource().isPresent())
                    throw new SQLException("Updating the Ticket in the database failed... the data source is not present.");
                Connection connection = getDataSource().get().getConnection();
                PreparedStatement statement = connection.prepareStatement(UPDATE_TICKET_QUERY);

                statement.setString(1, ticket.getOwner().toString());
                statement.setString(2, ticket.getAssignee().isPresent() ? ticket.getAssignee().get().toString() : null);
                statement.setTimestamp(3, Timestamp.from(ticket.getCreationDate()));
                statement.setTimestamp(4, Timestamp.from(ticket.getLastUpdateDate()));
                statement.setBoolean(5, ticket.isClosed());
                statement.setInt(6, ticket.getId());

                //Check if anything was affected, if it wasn't throw an exception.
                int rowsAffected = statement.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("Updating the Ticket in the database failed... no rows were affected.");
                }

                statement.close();
                connection.close();

                //Update all ticket messages in database also.
                ticket.getMessages().entrySet().forEach(message -> {
                    try {
                        updateTicketMessage(ticket, Instant.ofEpochMilli(message.getKey()), message.getValue());
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                });
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    public void deleteTicket(Ticket ticket) throws SQLException {
        executor.execute(() -> {
            try {
                if (!getDataSource().isPresent())
                    throw new SQLException("Deleting the Ticket in the database failed... the data source is not present.");
                Connection connection = getDataSource().get().getConnection();
                PreparedStatement statement = connection.prepareStatement(DELETE_TICKET_QUERY);

                statement.setInt(1, ticket.getId());

                //Check if anything was affected, if it wasn't throw an exception.
                int rowsAffected = statement.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("Deleting the Ticket in the database failed... no rows were affected.");
                }

                statement.close();
                connection.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    public void createTicketMessage(Ticket ticket, Instant creationDate, String message) throws SQLException {
        executor.execute(() -> {
            try {
                if (!getDataSource().isPresent()) throw new SQLException("Creating the Ticket Message in the database failed... the data source is not present.");
                Connection connection = getDataSource().get().getConnection();
                PreparedStatement statement = connection.prepareStatement(CREATE_TICKET_MESSAGE_QUERY);

                statement.setInt(1, ticket.getId());
                statement.setTimestamp(2, Timestamp.from(creationDate));
                statement.setString(3, message);

                //Check if anything was affected, if it wasn't throw an exception.
                int rowsAffected = statement.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("Creating the Ticket Message in the database failed... no rows were affected.");
                }

                statement.close();
                connection.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    public CompletableFuture<TreeMap<Long, String>> retrieveTicketMessages(int ticketID) throws SQLException {
        return cache.lookupTicketMessagesByTicketID(ticketID);
    }

    public void updateTicketMessage(Ticket ticket, Instant creationDate, String newMessage) throws SQLException {
        executor.execute(() -> {
            try {
                if (!getDataSource().isPresent())
                    throw new SQLException("Updating the Ticket Message in the database failed... the data source is not present.");
                Connection connection = getDataSource().get().getConnection();
                PreparedStatement statement = connection.prepareStatement(UPDATE_TICKET_MESSAGE_QUERY);

                statement.setString(1, newMessage);
                statement.setInt(2, ticket.getId());
                statement.setTimestamp(3, Timestamp.from(creationDate));

                //Check if anything was affected, if it wasn't throw an exception.
                int rowsAffected = statement.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("Updating the Ticket Message in the database failed... no rows were affected.");
                }

                statement.close();
                connection.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    public void deleteTicketMessage(Ticket ticket, Instant creationDate, String message) throws SQLException {
        executor.execute(() -> {
            try {
                if (!getDataSource().isPresent())
                    throw new SQLException("Deleting the Ticket Message in the database failed... the data source is not present.");
                Connection connection = getDataSource().get().getConnection();
                PreparedStatement statement = connection.prepareStatement(DELETE_TICKET_MESSAGE_QUERY);

                statement.setInt(1, ticket.getId());
                statement.setTimestamp(2, Timestamp.from(creationDate));
                statement.setString(3, message);

                //Check if anything was affected, if it wasn't throw an exception.
                int rowsAffected = statement.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("Deleting the Ticket in the database failed... no rows were affected.");
                }

                statement.close();
                connection.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    public TicketDataCache getCache() {
        return cache;
    }
}
