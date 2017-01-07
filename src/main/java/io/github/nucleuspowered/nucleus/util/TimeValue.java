/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.util;

import com.google.common.base.Preconditions;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * A Time value represents a unit of time. E.g. 4 seconds, 3 hours, 2 minutes, 1 year, etc.
 */
public class TimeValue implements Comparable<TimeValue> {

    private long value;
    private TimeUnit unit;

    /**
     * Construct a TimeValue
     *
     * @param value
     *  The value of time
     * @param unit
     *  The unit to use
     */
    public TimeValue(long value, TimeUnit unit) {
        this.value = value;
        this.unit = unit;
    }

    /**
     * @return
     *  The time unit as milliseconds
     */
    public long getAsMillis() {
        return this.unit.toMillis(this.value);
    }

    /**
     * @return
     *  The original passed in value
     */
    public long getValue() {
        return this.value;
    }

    /**
     * @return
     *  The unit of time
     */
    public TimeUnit getUnitOfMeasurement() {
        return this.unit;
    }

    /**
     * Compare this TimeValue to another TimeValue
     *
     * @param timeValue
     *  The other TimeValue to compare this too
     * @return
     *  The comparison
     */
    @Override
    public int compareTo(@Nonnull TimeValue timeValue) {
        Long thisAsMillis = this.getAsMillis();
        Long otherAsMillis = timeValue.getAsMillis();
        return thisAsMillis.compareTo(otherAsMillis);
    }

    /**
     * @return
     *  This TimeValue as a string
     */
    public String asString() {
        return String.format("%d %s", this.value, this.unit.name().toLowerCase());
    }

    /**
     * Converts the String to a TimeValue
     *
     * @param value
     *  The String to Parse
     * @return
     *  The TimeValue
     * @throws IllegalArgumentException
     *  Throws an IllegalArgumentException if we can't parse the string
     * @throws NullPointerException
     *  Throws a NullPointerException if the original String is null
     */
    public static TimeValue fromString(String value) throws IllegalArgumentException, NullPointerException {
        Preconditions.checkNotNull(value);
        if (!value.contains(" ")) {
            throw new IllegalArgumentException("No split character found!");
        }
        String[] split = value.split(" ");
        if (split.length != 2) {
            throw new IllegalArgumentException("Argument does not have the right directives!");
        }
        long timeValue;
        try {
            timeValue = Long.parseLong(split[0]);
        } catch (Exception e1) {
            throw new IllegalArgumentException(e1.getMessage(), e1.getCause());
        }
        TimeUnit unitOfMeasurement;
        try {
            unitOfMeasurement = TimeUnit.valueOf(split[1].toUpperCase());
        } catch (Exception e1) {
            throw new IllegalArgumentException(e1.getMessage(), e1.getCause());
        }
        return new TimeValue(timeValue, unitOfMeasurement);
    }
}
