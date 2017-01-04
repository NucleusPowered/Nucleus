/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Construct a List of Warning Times
 */
public class WarningTimeList {

    private List<TimeValue> sortedDelayTimes;

    public WarningTimeList() {
        this(new ArrayList<>());
    }

    public WarningTimeList(TimeValue ...values) {
        this(Arrays.asList(values));
    }

    /**
     * Constructs a list of sorted warning times
     *
     * @param values
     *  The list of time values to warn at
     */
    public WarningTimeList(List<TimeValue> values) {
        this.sortedDelayTimes = values.parallelStream().sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }

    /**
     * Gets a specific warning time
     *
     * @param place
     *  The index of the warning time to get
     * @return
     *  The warning time at a particular index
     */
    public TimeValue getWarningTimeAt(int place) {
        return this.sortedDelayTimes.get(place);
    }

    /**
     * @return
     *  the amount of warning times
     */
    public int getAmountOfWarnings() {
        return this.sortedDelayTimes.size();
    }

    /**
     * @return
     *  This WarningTimeList as a regular sorted list
     */
    public List<String> asList() {
        List<String> finalized = new ArrayList<>();
        this.sortedDelayTimes.forEach((timeValue -> finalized.add(timeValue.asString())));
        return finalized;
    }
}
