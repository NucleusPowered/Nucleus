/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ticket.filter.sql;


import io.github.nucleuspowered.nucleus.api.filter.FilterComparator;
import io.github.nucleuspowered.nucleus.modules.ticket.filter.FilterComparatorProperties;

/**
 * A set of filter comparator properties.
 */
public final class TicketQueryComparatorProperties {
    public static final FilterComparatorProperties LESS_THAN = new FilterComparatorProperties("<", 1);

    public static final FilterComparatorProperties LESS_THAN_OR_EQUAL_TO = new FilterComparatorProperties("<=", 1);

    public static final FilterComparatorProperties EQUALS = new FilterComparatorProperties("=", 1);

    public static final FilterComparatorProperties GREATER_THAN_OR_EQUAL_TO = new FilterComparatorProperties(">=", 1);

    public static final FilterComparatorProperties GREATER_THAN = new FilterComparatorProperties(">", 1);

    public static final FilterComparatorProperties BETWEEN = new FilterComparatorProperties("BETWEEN", 2);

    public static FilterComparatorProperties of(FilterComparator comparator) {
        switch (comparator) {
            case LESS_THAN:
                return TicketQueryComparatorProperties.LESS_THAN;
            case LESS_THAN_OR_EQUAL_TO:
                return TicketQueryComparatorProperties.LESS_THAN_OR_EQUAL_TO;
            case EQUALS:
                return TicketQueryComparatorProperties.EQUALS;
            case GREATER_THAN_OR_EQUAL_TO:
                return TicketQueryComparatorProperties.GREATER_THAN_OR_EQUAL_TO;
            case GREATER_THAN:
                return TicketQueryComparatorProperties.GREATER_THAN;
            case BETWEEN:
                return TicketQueryComparatorProperties.BETWEEN;
            default:
                return null;
        }
    }
}
