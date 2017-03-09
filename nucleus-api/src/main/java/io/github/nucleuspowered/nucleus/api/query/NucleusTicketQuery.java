/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.query;

import com.google.common.collect.ImmutableMap;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.util.ResettableBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public interface NucleusTicketQuery {

    /**
     * Creates a {@link PreparedStatement} using the query filters provided by the {@link Builder}.
     *
     * @param connection The connection to use when creating this query.
     * @return A {@link PreparedStatement}.
     */
    PreparedStatement constructQuery(Connection connection) throws SQLException;

    /**
     * Creates an SQL query string with replacement parameters using the query filters provided by the {@link Builder}.
     *
     * @return An SQL query.
     */
    String createPlainQueryString();

    /**
     * Creates a map containing parameters mapped to the value which needs to be replaced in the final query string - in
     * order.
     *
     * @return A map of replacement parameters.
     */
    Map<Integer, Object> createParameterReplacementList();

    /**
     * Gets the raw queries obtained from the {@link Builder} being used to construct the query string and replacement
     * parameters map.
     *
     * @return A map of queries obtained from the builder.
     */
    ImmutableMap<String, Map<Integer, Object>> getQueries();

    static Builder builder() {
        return Sponge.getRegistry().createBuilder(NucleusTicketQuery.Builder.class);
    }

    /**
     * A builder which can create a {@link NucleusTicketQuery}.
     */
    interface Builder extends ResettableBuilder<NucleusTicketQuery, Builder> {

        /**
         * Sets the column to query for data.
         *
         * @param column The {@link Column}.
         * @return this builder.
         */
        Builder column(Column column);

        /**
         * Sets the method in which to compare data to filter what is returned.
         *
         * @param comparator The {@link QueryComparator}.
         * @return this builder.
         */
        Builder comparator(QueryComparator comparator);

        /**
         * Sets the value used in filtering the previously provided column with the previously provided comparator. The
         * value must be of the type expected by the column.
         *
         * @param value The value to filter by in the query.
         * @return this builder.
         */
        Builder value(Object value);

        /**
         * Replaces a previously set value used in filtering the previously provided column with the previously provided
         * comparator. The value must be of the type expected by the column.
         *
         * @param valueIndex The index in which to replace the value.
         * @param value      The value to filter by in the query.
         * @return this builder.
         */
        Builder replaceValue(int valueIndex, Object value);

        /**
         * Validates all provided input for the column, comparator and value. A query is then constructed if the
         * provided data is valid. A new filter can be built after this method has been called.
         *
         * @return this builder.
         */
        Builder completeFilter();

        /**
         * Simplified method to quickly construct a filter for a ticket query.
         *
         * @param column     The {@link Column} to query.
         * @param comparator The {@link QueryComparator} to use for filtering.
         * @param values     The value(s) to filter by.
         * @return this builder.
         */
        Builder filter(Column column, QueryComparator comparator, Object... values);

        /**
         * Gets all queries and replacement tokens built by this builder.
         *
         * @return A map of queries mapped to their replacement tokens.
         */
        Map<String, Map<Integer, Object>> getQueries();

        /**
         * Creates a {@link NucleusTicketQuery} with the data collected by this builder.
         *
         * @return A {@link NucleusTicketQuery}.
         */
        NucleusTicketQuery build();
    }

    /**
     * An enumeration of columns which can be queried for Ticket related data.
     */
    enum Column {
        /**
         * Option to query by a the 'ID' column in the 'Tickets' table.
         */
        ID,

        /**
         * Option to query by a the 'Owner' column in the 'Tickets' table.
         */
        OWNER,

        /**
         * Option to query by a the 'Assignee' column in the 'Tickets' table.
         */
        ASSIGNEE,

        /**
         * Option to query by a the 'CreationDate' column in the 'Tickets' table.
         */
        CREATION_DATE,

        /**
         * Option to query by a the 'LastUpdateDate' column in the 'Tickets' table.
         */
        LAST_UPDATE_DATE,

        /**
         * Option to query by a the 'Closed' column in the 'Tickets' table.
         */
        STATUS
    }
}
