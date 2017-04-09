/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ticket.filter;

import com.google.common.collect.ImmutableList;
import io.github.nucleuspowered.nucleus.api.filter.FilterComparator;
import io.github.nucleuspowered.nucleus.api.filter.NucleusTicketFilter;

import java.util.ArrayList;

public class TicketFilter implements NucleusTicketFilter {
    private final Property property;
    private final FilterComparator comparator;
    private final ImmutableList<Object> values;

    public TicketFilter(Property property, FilterComparator comparator, ArrayList<Object> values) {
        this.property = property;
        this.comparator = comparator;
        this.values = ImmutableList.copyOf(values);
    }

    @Override
    public Property getProperty() {
        return property;
    }

    @Override
    public FilterComparator getComparator() {
        return comparator;
    }

    @Override
    public ImmutableList<Object> getValues() {
        return values;
    }
}
