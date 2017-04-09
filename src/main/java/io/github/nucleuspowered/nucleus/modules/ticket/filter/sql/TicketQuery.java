/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ticket.filter.sql;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.api.filter.NucleusTicketFilter;

import java.sql.*;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TicketQuery {
    private final ImmutableMap<String, ImmutableList<Object>> queries;

    public TicketQuery(Set<NucleusTicketFilter> filters) {
        queries = ImmutableMap.copyOf(initializeFromFilters(filters));
    }

    /**
     * Turns a set of {@link NucleusTicketFilter}s into a map of partial query strings to a list of objects which are
     * replacement parameters for the partial query string.
     *
     * @param filters The set of {@link NucleusTicketFilter}s to convert.
     * @return The map of converted filters.
     */
    public Map<String, ImmutableList<Object>> initializeFromFilters(Set<NucleusTicketFilter> filters) {
        Map<String, ImmutableList<Object>> queries = Maps.newHashMap();

        filters.forEach(filter -> {
            switch (filter.getComparator()) {
                case BETWEEN:
                    queries.put(TicketQuery.TicketColumnProperties.of(filter.getProperty()).getColumnName() + " " + TicketQueryComparatorProperties.of(filter.getComparator()).getComparator() + " ? AND ?", //Since this will be used to create a prepared statement use a token instead of the value.
                            filter.getValues());
                    break;
                default:
                    queries.put(TicketQuery.TicketColumnProperties.of(filter.getProperty()).getColumnName() + " " + TicketQueryComparatorProperties.of(filter.getComparator()).getComparator() + " ?", //Since this will be used to create a prepared statement use a token instead of the value.
                            filter.getValues());
                    break;
            }
        });

        return queries;
    }

    /**
     * Creates a {@link PreparedStatement} using the query created by this instance.
     *
     * @param connection The connection to use when creating this query.
     * @return A {@link PreparedStatement}.
     */
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

    /**
     * Creates an SQL query string with replacement parameters using the query created by this instance.
     *
     * @return An SQL query.
     */
    public String createPlainQueryString() {
        String query = "SELECT * FROM Tickets";
        if (queries.size() > 0) {
            query += " WHERE ";
            Iterator<Map.Entry<String, ImmutableList<Object>>> entryIterator = queries.entrySet().iterator();
            while (entryIterator.hasNext()) {
                Map.Entry<String, ImmutableList<Object>> argument = entryIterator.next();

                //Add the filter filter to the SQL filter.
                query += "(" + argument.getKey() + ")";
                if (entryIterator.hasNext()) query += " AND ";
            }
        }
        query += ";";

        return query;
    }

    /**
     * Creates a map containing parameters mapped to the value which needs to be replaced in the final query string - in
     * order.
     *
     * @return A map of replacement parameters.
     */
    public Map<Integer, Object> createParameterReplacementList() {
        Map<Integer, Object> replacementParameters = Maps.newHashMap();

        for (ImmutableList<Object> singleQueryParameters : queries.values()) {
            int baseSize = replacementParameters.size() + 1;
            singleQueryParameters.stream().forEach(o -> replacementParameters.put(baseSize + singleQueryParameters.indexOf(o), o));
        }

        return ImmutableMap.copyOf(replacementParameters);
    }

    public static TicketQuery fromFilters(Set<NucleusTicketFilter> filters) {
        return new TicketQuery(filters);
    }

    /**
     * A set of column properties for the tickets table.
     */
    public final static class TicketColumnProperties {
        public static final io.github.nucleuspowered.nucleus.modules.ticket.filter.sql.TicketColumnProperties ID = new io.github.nucleuspowered.nucleus.modules.ticket.filter.sql.TicketColumnProperties("ID", JDBCType.INTEGER, Integer.class);

        public static final io.github.nucleuspowered.nucleus.modules.ticket.filter.sql.TicketColumnProperties OWNER = new io.github.nucleuspowered.nucleus.modules.ticket.filter.sql.TicketColumnProperties("Owner", JDBCType.CHAR, UUID.class);

        public static final io.github.nucleuspowered.nucleus.modules.ticket.filter.sql.TicketColumnProperties ASSIGNEE = new io.github.nucleuspowered.nucleus.modules.ticket.filter.sql.TicketColumnProperties("Assignee", JDBCType.CHAR, UUID.class);

        public static final io.github.nucleuspowered.nucleus.modules.ticket.filter.sql.TicketColumnProperties CREATION_DATE = new io.github.nucleuspowered.nucleus.modules.ticket.filter.sql.TicketColumnProperties("CreationDate", JDBCType.TIMESTAMP, Instant.class);

        public static final io.github.nucleuspowered.nucleus.modules.ticket.filter.sql.TicketColumnProperties LAST_UPDATE_DATE = new io.github.nucleuspowered.nucleus.modules.ticket.filter.sql.TicketColumnProperties("LastUpdateDate", JDBCType.TIMESTAMP, Instant.class);

        public static final io.github.nucleuspowered.nucleus.modules.ticket.filter.sql.TicketColumnProperties STATUS = new io.github.nucleuspowered.nucleus.modules.ticket.filter.sql.TicketColumnProperties("Closed", JDBCType.BOOLEAN, Boolean.class);

        public static io.github.nucleuspowered.nucleus.modules.ticket.filter.sql.TicketColumnProperties of(NucleusTicketFilter.Property column) {
            switch (column) {
                case ID:
                    return TicketQuery.TicketColumnProperties.ID;
                case OWNER:
                    return TicketQuery.TicketColumnProperties.OWNER;
                case ASSIGNEE:
                    return TicketQuery.TicketColumnProperties.ASSIGNEE;
                case CREATION_DATE:
                    return TicketQuery.TicketColumnProperties.CREATION_DATE;
                case LAST_UPDATE_DATE:
                    return TicketQuery.TicketColumnProperties.LAST_UPDATE_DATE;
                case STATUS:
                    return TicketQuery.TicketColumnProperties.STATUS;
                default:
                    return null;
            }
        }
    }
}
