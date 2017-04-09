/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.filter;

import com.google.common.collect.ImmutableList;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.util.ResettableBuilder;

import java.util.Set;

public interface NucleusTicketFilter {

    /**
     * Gets the property that is filtered.
     *
     * @return The {@link Property}.
     */
    Property getProperty();

    /**
     * Gets the comparator used to filter the property.
     *
     * @return The {@link FilterComparator}.
     */
    FilterComparator getComparator();

    /**
     * Gets the value(s) that are used to filter the property.
     *
     * @return An array list of values.
     */
    ImmutableList<Object> getValues();

    static Builder builder() {
        return Sponge.getRegistry().createBuilder(NucleusTicketFilter.Builder.class);
    }

    /**
     * A builder which can create a set of {@link NucleusTicketFilter}s.
     */
    interface Builder extends ResettableBuilder<Set<NucleusTicketFilter>, Builder> {

        /**
         * Sets the property to filter by.
         *
         * @param column The {@link Property}.
         * @return this builder.
         */
        Builder property(Property column);

        /**
         * Sets the method in which to compare data to filter what is returned.
         *
         * @param comparator The {@link FilterComparator}.
         * @return this builder.
         */
        Builder comparator(FilterComparator comparator);

        /**
         * Sets the value used in filtering the previously provided property with the previously provided comparator. The
         * value must be of the type expected by the property.
         *
         * @param value The value to filter by.
         * @return this builder.
         */
        Builder value(Object value);

        /**
         * Replaces a previously set value used in filtering the previously provided property with the previously provided
         * comparator. The value must be of the type expected by the column.
         *
         * @param valueIndex The index in which to replace the value.
         * @param value      The new value to filter by.
         * @return this builder.
         */
        Builder replaceValue(int valueIndex, Object value);

        /**
         * Validates all provided input for the column, comparator and value. A filter is then constructed if the
         * provided data is valid. A new filter can be built after this method has been called.
         *
         * @return this builder.
         */
        Builder completeFilter();

        /**
         * Simplified method to quickly construct a filter for tickets.
         *
         * @param property     The {@link Property} to filter.
         * @param comparator The {@link FilterComparator} to use for filtering.
         * @param values     The value(s) to filter by.
         * @return this builder.
         */
        Builder filter(Property property, FilterComparator comparator, Object... values);

        /**
         * Creates a set of {@link NucleusTicketFilter}s with the data collected by this builder.
         *
         * @return A set of {@link NucleusTicketFilter}s.
         */
        Set<NucleusTicketFilter> build();
    }

    /**
     * An enumeration of properties which can be used as filters for Ticket related data.
     */
    enum Property {
        /**
         * Option to filter by the Ticket 'ID'.
         */
        ID,

        /**
         * Option to filter by the Ticket 'Owner'.
         */
        OWNER,

        /**
         * Option to filter by the Ticket 'Assignee'.
         */
        ASSIGNEE,

        /**
         * Option to filter by the Ticket 'CreationDate'.
         */
        CREATION_DATE,

        /**
         * Option to filter by the Ticket 'LastUpdateDate'.
         */
        LAST_UPDATE_DATE,

        /**
         * Option to filter by the Ticket 'Status'.
         */
        STATUS
    }
}
