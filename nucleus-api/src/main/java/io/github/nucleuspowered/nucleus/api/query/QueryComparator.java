/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.query;

/**
 * An enumeration of comparators that can be used in a query.
 */
public enum QueryComparator {

    /**
     * Compares if a value is less than another value.
     */
    LESS_THAN,

    /**
     * Compares if a value is less than or equal to another value.
     */
    LESS_THAN_OR_EQUAL_TO,

    /**
     * Compares if a value is equal to another value.
     */
    EQUALS,

    /**
     * Compares if a value is greater than or equal to another value.
     */
    GREATER_THAN_OR_EQUAL_TO,

    /**
     * Compares if a value is greater than another value.
     */
    GREATER_THAN,

    /**
     * Compares if a timestamp is between a certain range of timestamps.
     */
    BETWEEN
}
