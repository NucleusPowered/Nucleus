/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ticket.filter.sql;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.github.nucleuspowered.nucleus.api.filter.FilterComparator;
import io.github.nucleuspowered.nucleus.api.filter.NucleusTicketFilter;
import io.github.nucleuspowered.nucleus.modules.ticket.filter.TicketFilter;

import java.util.ArrayList;
import java.util.Set;

public class TicketQueryBuilder implements NucleusTicketFilter.Builder {
    private Set<NucleusTicketFilter> filters = Sets.newHashSet();
    private NucleusTicketFilter.Property column;
    private FilterComparator comparator;
    private ArrayList<Object> values = Lists.newArrayList();

    public TicketQueryBuilder() {
        reset();
    }

    @Override
    public NucleusTicketFilter.Builder property(NucleusTicketFilter.Property column) {
        Preconditions.checkState(TicketQuery.TicketColumnProperties.of(column) != null,
                "The column " + column.toString() + " does not have a valid set of column properties.");

        this.column = column;
        return this;
    }

    @Override
    public NucleusTicketFilter.Builder comparator(FilterComparator comparator) {
        this.comparator = comparator;
        return this;
    }

    @Override
    public NucleusTicketFilter.Builder value(Object value) {
        Preconditions.checkState(column != null,
                "You must first declare the column you are querying before declaring a value to filter by.");
        Preconditions.checkState(comparator != null,
                "You must first declare the comparator you are using to filter by before declaring a value to use with the filter.");
        Preconditions.checkState(values.size() < TicketQueryComparatorProperties.of(comparator).getExpectedValueCount(),
                "This filter already has the maximum number of assigned values.");
        Preconditions.checkState(TicketQuery.TicketColumnProperties.of(column).getJavaType().isAssignableFrom(value.getClass()),
                "The value is not of the expected type, the column " + column.toString() + " expects a filter parameter of type " +
                        TicketQuery.TicketColumnProperties.of(column).getJavaType().getSimpleName());

        values.add(value);
        return this;
    }

    @Override
    public NucleusTicketFilter.Builder replaceValue(int valueIndex, Object value) {
        Preconditions.checkState(column != null,
                "You must first declare the column you are querying before declaring a value to filter by.");
        Preconditions.checkState(comparator != null,
                "You must first declare the comparator you are using to filter by before declaring a value to use with the filter.");
        Preconditions.checkState(valueIndex > 0,
                "The value index must be greater than 0.");
        Preconditions.checkState(valueIndex <= TicketQueryComparatorProperties.of(comparator).getExpectedValueCount(),
                "The provided value index is greater than the maximum possible number of values.");
        Preconditions.checkState(TicketQuery.TicketColumnProperties.of(column).getJavaType().isAssignableFrom(value.getClass()),
                "The value is not of the expected type, the column " + column.toString() + " expects a filter parameter of type " +
                        TicketQuery.TicketColumnProperties.of(column).getJavaType().getSimpleName());

        values.set(valueIndex, value);
        return this;
    }

    @Override
    public NucleusTicketFilter.Builder completeFilter() {
        Preconditions.checkState(column != null,
                "The column to filter must be defined.");
        Preconditions.checkState(comparator != null,
                "The comparator to use in the filter must be defined.");
        Preconditions.checkState(values.size() == TicketQueryComparatorProperties.of(comparator).getExpectedValueCount(),
                "The comparator expects " + TicketQueryComparatorProperties.of(comparator).getExpectedValueCount() + " value(s) but " +
                        values.size() + " value(s) were provided.");


        filters.add(new TicketFilter(column, comparator, Lists.newArrayList(values)));

        this.column = null;
        this.comparator = null;
        values.clear();

        return this;
    }

    @Override
    public NucleusTicketFilter.Builder filter(NucleusTicketFilter.Property column, FilterComparator comparator, Object... values) {
        property(column);
        comparator(comparator);
        for (Object value : values) {
            value(value);
        }
        completeFilter();

        return this;
    }

    @Override
    public Set<NucleusTicketFilter> build() {
        return filters;
    }

    @Override
    public NucleusTicketFilter.Builder from(Set<NucleusTicketFilter> value) {
        this.filters = value;
        return this;
    }

    @Override
    public NucleusTicketFilter.Builder reset() {
        filters.clear();
        column = null;
        comparator = null;
        values.clear();
        return this;
    }
}
