/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ticket.filter;


/**
 * A set of properties possessed by a {@link FilterComparatorProperties}.
 */
public class FilterComparatorProperties {
    private final String comparator;
    private final int expectedValueCount;

    public FilterComparatorProperties(String comparator, int expectedValueCount) {
        this.comparator = comparator;
        this.expectedValueCount = expectedValueCount;
    }

    /**
     * The character or string which must be used in a filter to represent this comparator.
     *
     * @return The character or string of this comparator.
     */

    public String getComparator() {
        return comparator;
    }

    /**
     * The amount of values this comparator requires in order to function.
     *
     * @return The number of values.
     */
    public int getExpectedValueCount() {
        return expectedValueCount;
    }
}