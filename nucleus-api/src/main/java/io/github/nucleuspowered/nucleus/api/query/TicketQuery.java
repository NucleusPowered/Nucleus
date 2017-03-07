/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.query;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.sql.*;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class TicketQuery {
    private final Map<String, Map<Integer, Object>> queries;

    public TicketQuery(Builder builder) {
        this.queries = ImmutableMap.copyOf(builder.queries);
    }

    /**
     * Creates a {@link PreparedStatement} using the query filters provided by the {@link Builder}.
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
            } catch (SQLException ex) { }
        });

        return statement;
    }

    /**
     * Creates an SQL query string with replacement parameters using the query filters provided by the {@link Builder}.
     *
     * @return An SQL query.
     */
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

    /**
     * Creates a map containing parameters mapped to the value which needs to be replaced in the final query string - in
     * order.
     *
     * @return A map of replacement parameters.
     */
    public Map<Integer, Object> createParameterReplacementList() {
        Map<Integer, Object> replacementParameters = Maps.newHashMap();

        for (Map<Integer, Object> singleQueryParameters : queries.values()) {
            int baseSize = replacementParameters.size();
            singleQueryParameters.entrySet().forEach(qp -> replacementParameters.put(baseSize + qp.getKey(), qp.getValue()));
        }

        return ImmutableMap.copyOf(replacementParameters);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder which can create a {@link TicketQuery}.
     */
    public static class Builder {
        private Map<String, Map<Integer, Object>> queries = Maps.newHashMap(); //The value is a map of the parameter index for the prepared statement and the value to replace.
        private Column column;
        private QueryComparator comparator;
        private Map<Integer, Object> values = Maps.newHashMap();

        /**
         * Sets the column to query for data.
         *
         * @param column The {@link Column}.
         * @return this builder.
         */
        public Builder column(Column column) {
            this.column = column;
            return this;
        }

        /**
         * Sets the method in which to compare data to filter what is returned.
         *
         * @param comparator The {@link QueryComparator}.
         * @return this builder.
         */
        public Builder comparator(QueryComparator comparator) {
            this.comparator = comparator;
            return this;
        }

        /**
         * Sets the value used in filtering the previously provided column with the previously provided comparator. The
         * value must be of the type expected by the column.
         *
         * @param value The value to filter by in the query.
         * @return this builder.
         */
        public Builder value(Object value) {
            Preconditions.checkState(column != null,
                    "You must first declare the column you are querying before declaring a value to filter by.");
            Preconditions.checkState(comparator != null,
                    "You must first declare the comparator you are using to filter by before declaring a value to use with the filter.");
            Preconditions.checkState(values.size() < comparator.getExpectedValueCount(),
                    "This query already has the maximum number of assigned values.");
            Preconditions.checkState(column.getJavaType().isAssignableFrom(value.getClass()),
                    "The value is not of the expected type, the column " + column.toString() + " expects a query parameter of type " +
                            column.getJavaType().getSimpleName());

            replaceValue(values.size() + 1, value); //Add one to the size because the parameter index starts at 1, not 0.
            return this;
        }

        /**
         * Replaces a previously set value used in filtering the previously provided column with the previously provided
         * comparator. The value must be of the type expected by the column.
         *
         * @param valueIndex The index in which to replace the value.
         * @param value      The value to filter by in the query.
         * @return this builder.
         */
        public Builder replaceValue(int valueIndex, Object value) {
            Preconditions.checkState(column != null,
                    "You must first declare the column you are querying before declaring a value to filter by.");
            Preconditions.checkState(comparator != null,
                    "You must first declare the comparator you are using to filter by before declaring a value to use with the filter.");
            Preconditions.checkState(valueIndex > 0,
                    "The value index must be greater than 0.");
            Preconditions.checkState(valueIndex <= comparator.getExpectedValueCount(),
                    "The provided value index is greater than the maximum possible number of values.");
            Preconditions.checkState(column.getJavaType().isAssignableFrom(value.getClass()),
                    "The value is not of the expected type, the column " + column.toString() + " expects a query parameter of type " +
                            column.getJavaType().getSimpleName());

            values.put(valueIndex, value);
            return this;
        }

        /**
         * Validates all provided input for the column, comparator and value. A query is then constructed if the
         * provided data is valid. A new filter can be built after this method has been called.
         *
         * @return this builder.
         */
        public Builder completeFilter() {
            Preconditions.checkState(column != null,
                    "The column to query must be defined.");
            Preconditions.checkState(comparator != null,
                    "The comparator to use in the query must be defined.");
            Preconditions.checkState(values.size() == comparator.getExpectedValueCount(),
                    "The comparator expects " + comparator.getExpectedValueCount() + " value(s) but " +
                            values.size() + " value(s) were provided.");

            switch (comparator) {
                case BETWEEN:
                    queries.put(column.getColumnName() + " " + comparator.getComparator() + " ? AND ?", ImmutableMap.copyOf(values)); //Since this will be used to create a prepared statement use a token instead of the value.
                    break;
                default:
                    queries.put(column.getColumnName() + " " + comparator.getComparator() + " ?", ImmutableMap.copyOf(values)); //Since this will be used to create a prepared statement use a token instead of the value.
                    break;
            }

            this.column = null;
            this.comparator = null;
            values.clear();
            return this;
        }

        /**
         * Simplified method to quickly construct a filter for a ticket query.
         *
         * @param column     The {@link Column} to query.
         * @param comparator The {@link QueryComparator} to use for filtering.
         * @param values     The value(s) to filter by.
         * @return this builder.
         */
        public Builder filter(Column column, QueryComparator comparator, Object... values) {
            column(column);
            comparator(comparator);
            for (Object value : values) {
                value(value);
            }
            completeFilter();

            return this;
        }

        /**
         * Creates a {@link TicketQuery} with the data collected by this builder.
         *
         * @return A {@link TicketQuery}.
         */
        public TicketQuery build() {
            return new TicketQuery(this);
        }
    }

    /**
     * An enumeration of columns which can be queried for Ticket related data.
     */
    public enum Column {
        /**
         * Option to query by a the 'ID' column in the 'Tickets' table. The value must be of the {@link Integer} type.
         */
        ID("ID", JDBCType.INTEGER, Integer.class),

        /**
         * Option to query by a the 'Owner' column in the 'Tickets' table. The value must be of the {@link UUID} type.
         */
        OWNER("Owner", JDBCType.CHAR, UUID.class),

        /**
         * Option to query by a the 'Assignee' column in the 'Tickets' table. The value must be of the {@link UUID}
         * type.
         */
        ASSIGNEE("Assignee", JDBCType.CHAR, UUID.class),

        /**
         * Option to query by a the 'CreationDate' column in the 'Tickets' table by a range of dates. The value must be
         * of the {@link Instant} type.
         */
        CREATION_DATE("CreationDate", JDBCType.TIMESTAMP, Instant.class),

        /**
         * Option to query by a the 'LastUpdateDate' column in the 'Tickets' table by a range of dates. The value must
         * be of the {@link Instant} type.
         */
        LAST_UPDATE_DATE("LastUpdateDate", JDBCType.TIMESTAMP, Instant.class),

        /**
         * Option to query by a the 'Closed' column in the 'Tickets' table. The value must be of the {@link Boolean}
         * type.
         */
        STATUS("Closed", JDBCType.BOOLEAN, Boolean.class);

        private final String columnName;
        private final SQLType sqlType;
        private final Class javaType;

        Column(String columnName, SQLType sqlType, Class javaType) {
            this.columnName = columnName;
            this.sqlType = sqlType;
            this.javaType = javaType;
        }

        public String getColumnName() {
            return columnName;
        }

        public SQLType getSqlType() {
            return sqlType;
        }

        public Class getJavaType() {
            return javaType;
        }
    }
}
