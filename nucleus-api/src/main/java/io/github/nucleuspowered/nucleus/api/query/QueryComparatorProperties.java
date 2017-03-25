/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.query;

/**
 * A set of properties possessed by a {@link QueryComparator}.
 */
public interface QueryComparatorProperties {

    /**
     * The character or string which must be used in a query to represent this comparator.
     *
     * @return The character or string of this comparator.
     */
    String getComparator();

    /**
     * The amount of values this comparator requires in order to function.
     *
     * @return The number of values.
     */
    int getExpectedValueCount();
}
