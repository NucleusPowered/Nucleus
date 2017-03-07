package io.github.nucleuspowered.nucleus.api.query;

public enum QueryComparator {

    /**
     * General comparators.
     */
    LESS_THAN("<", 1),
    LESS_THAN_OR_EQUAL_TO("<=", 1),
    EQUALS("=", 1),
    GREATER_THAN_OR_EQUAL_TO(">=", 1),
    GREATER_THAN(">", 1),

    /**
     * Compares if a timestamp is between a certain range of timestamps.
     */
    BETWEEN("BETWEEN", 2);

    private final String comparator;
    private final int expectedValueCount;

    QueryComparator(String comparator, int expectedValueCount) {
        this.comparator = comparator;
        this.expectedValueCount = expectedValueCount;

    }

    public String getComparator() {
        return comparator;
    }

    public int getExpectedValueCount() {
        return expectedValueCount;
    }
}
