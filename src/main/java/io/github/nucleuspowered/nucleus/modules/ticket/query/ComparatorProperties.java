package io.github.nucleuspowered.nucleus.modules.ticket.query;

import io.github.nucleuspowered.nucleus.api.query.QueryComparatorProperties;

public class ComparatorProperties implements QueryComparatorProperties {
    private final String comparator;
    private final int expectedValueCount;

    public ComparatorProperties(String comparator, int expectedValueCount) {
        this.comparator = comparator;
        this.expectedValueCount = expectedValueCount;
    }

    @Override
    public String getComparator() {
        return comparator;
    }

    @Override
    public int getExpectedValueCount() {
        return expectedValueCount;
    }
}
