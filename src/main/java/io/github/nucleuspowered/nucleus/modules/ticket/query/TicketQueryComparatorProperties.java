/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ticket.query;


import io.github.nucleuspowered.nucleus.api.query.QueryComparator;
import io.github.nucleuspowered.nucleus.api.query.QueryComparatorProperties;

/**
 * A set of query comparator properties.
 */
public final class TicketQueryComparatorProperties {

    public static final QueryComparatorProperties LESS_THAN = new ComparatorProperties("<", 1);

    public static final QueryComparatorProperties LESS_THAN_OR_EQUAL_TO = new ComparatorProperties("<=", 1);

    public static final QueryComparatorProperties EQUALS = new ComparatorProperties("=", 1);

    public static final QueryComparatorProperties GREATER_THAN_OR_EQUAL_TO = new ComparatorProperties(">=", 1);

    public static final QueryComparatorProperties GREATER_THAN = new ComparatorProperties(">", 1);

    public static final QueryComparatorProperties BETWEEN = new ComparatorProperties("BETWEEN", 2);

    public static QueryComparatorProperties of(QueryComparator comparator) {
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
