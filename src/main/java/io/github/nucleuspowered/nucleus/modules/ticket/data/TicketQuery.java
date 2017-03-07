package io.github.nucleuspowered.nucleus.modules.ticket.data;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.api.query.NucleusTicketQuery;
import io.github.nucleuspowered.nucleus.api.query.QueryColumnProperties;

import java.sql.*;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class TicketQuery implements NucleusTicketQuery {
    private final ImmutableMap<String, Map<Integer, Object>> queries;

    public TicketQuery(NucleusTicketQuery.Builder builder) {
        this.queries = ImmutableMap.copyOf(builder.getQueries());
    }

    @Override
    public PreparedStatement constructQuery(Connection connection) throws SQLException {
        String query = createPlainQueryString();
        Map<Integer, Object> parameters = createParameterReplacementList();

        PreparedStatement statement = connection.prepareStatement(query);
        parameters.entrySet().forEach(entry -> {
            int parameterIndex = entry.getKey();
            Object value = entry.getValue();
            try {
                //This currently only contains the values used by the columns in the Tickets table, add more if required.
                if (value == null) {
                    statement.setNull(parameterIndex, Types.NULL);
                } else if (value instanceof Integer) {
                    statement.setInt(parameterIndex, (int) value);
                } else if (value instanceof UUID) {
                    statement.setString(parameterIndex, value.toString()); //Since in our DB Tables we are representing a UUID as a 36 character string, set a string value.
                } else if (value instanceof Instant) {
                    statement.setTimestamp(parameterIndex, Timestamp.from((Instant) value));
                } else if (value instanceof Boolean) {
                    statement.setBoolean(parameterIndex, (boolean) value);
                }
            } catch (SQLException ex) {
            }
        });

        return statement;
    }

    @Override
    public String createPlainQueryString() {
        String query = "SELECT * FROM Tickets";
        if (queries.size() > 0) {
            query += " WHERE ";
            Iterator<Map.Entry<String, Map<Integer, Object>>> entryIterator = queries.entrySet().iterator();
            while (entryIterator.hasNext()) {
                Map.Entry<String, Map<Integer, Object>> argument = entryIterator.next();

                //Add the query filter to the SQL query.
                query += "(" + argument.getKey() + ")";
                if (entryIterator.hasNext()) query += " AND ";
            }
        }
        query += ");";

        return query;
    }

    @Override
    public Map<Integer, Object> createParameterReplacementList() {
        Map<Integer, Object> replacementParameters = Maps.newHashMap();

        for (Map<Integer, Object> singleQueryParameters : queries.values()) {
            int baseSize = replacementParameters.size();
            singleQueryParameters.entrySet().forEach(qp -> replacementParameters.put(baseSize + qp.getKey(), qp.getValue()));
        }

        return ImmutableMap.copyOf(replacementParameters);
    }

    @Override
    public ImmutableMap<String, Map<Integer, Object>> getQueries() {
        return queries;
    }

    /**
     * A set of column properties for the tickets table.
     */
    public final static class TicketColumnProperties {

        public static final QueryColumnProperties ID = new ColumnProperties("ID", JDBCType.INTEGER, Integer.class);

        public static final QueryColumnProperties OWNER = new ColumnProperties("Owner", JDBCType.CHAR, UUID.class);

        public static final QueryColumnProperties ASSIGNEE = new ColumnProperties("Assignee", JDBCType.CHAR, UUID.class);

        public static final QueryColumnProperties CREATION_DATE = new ColumnProperties("CreationDate", JDBCType.TIMESTAMP, Instant.class);

        public static final QueryColumnProperties LAST_UPDATE_DATE = new ColumnProperties("LastUpdateDate", JDBCType.TIMESTAMP, Instant.class);

        public static final QueryColumnProperties STATUS = new ColumnProperties("Closed", JDBCType.BOOLEAN, Boolean.class);

        public static QueryColumnProperties getColumnProperties(NucleusTicketQuery.Column column) {
            switch (column) {
                case ID:
                    return TicketColumnProperties.ID;
                case OWNER:
                    return TicketColumnProperties.OWNER;
                case ASSIGNEE:
                    return TicketColumnProperties.ASSIGNEE;
                case CREATION_DATE:
                    return TicketColumnProperties.CREATION_DATE;
                case LAST_UPDATE_DATE:
                    return TicketColumnProperties.LAST_UPDATE_DATE;
                case STATUS:
                    return TicketColumnProperties.STATUS;
                default:
                    return null;
            }
        }
    }
}
