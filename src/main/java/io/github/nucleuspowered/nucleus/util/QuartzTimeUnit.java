/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.util;

import javax.annotation.Nullable;

public enum QuartzTimeUnit {
    DAY_OF_WEEK,
    DAY_OF_MONTH,
    MONTHS,
    HOURS,
    MINUTES,
    SECONDS;

    /**
     * An alternative method to valueOf accounting for multiple spellings of an enum value
     *
     * @param asString
     *  The value as a String
     * @return
     *  The QuartzTimeUnit or null if we couldn't find an acceptable match
     */
    @Nullable
    public static QuartzTimeUnit fromString(String asString) {
        if (asString.equalsIgnoreCase("dow") || asString.equalsIgnoreCase("day_of_week") ||
                asString.equalsIgnoreCase("day-of-week") || asString.equalsIgnoreCase("days_of_the_week") ||
                asString.equalsIgnoreCase("days-of-the-week")) {
            return QuartzTimeUnit.DAY_OF_WEEK;
        }
        if (asString.equalsIgnoreCase("dom") || asString.equalsIgnoreCase("day_of_month") ||
                asString.equalsIgnoreCase("day-of-month") || asString.equalsIgnoreCase("days_of_the_month") ||
                asString.equalsIgnoreCase("days-of-the-month")) {
            return QuartzTimeUnit.DAY_OF_MONTH;
        }
        if (asString.equalsIgnoreCase("months") || asString.equalsIgnoreCase("month")) {
            return QuartzTimeUnit.MONTHS;
        }
        if (asString.equalsIgnoreCase("hours") || asString.equalsIgnoreCase("hour")) {
            return QuartzTimeUnit.HOURS;
        }
        if (asString.equalsIgnoreCase("minutes") || asString.equalsIgnoreCase("minute")) {
            return QuartzTimeUnit.MINUTES;
        }
        if (asString.equalsIgnoreCase("seconds") || asString.equalsIgnoreCase("second")) {
            return QuartzTimeUnit.SECONDS;
        }

        return null;
    }
}
