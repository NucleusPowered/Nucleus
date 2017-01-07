/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.util;

import com.cronutils.model.Cron;

import java.util.Optional;

/**
 * Used for serialization purposes inside configs. Provides one interface
 * for multiple delayed time types (cron, when time, etc.)
 */
public class DelayedTimeValue {

    private DelayedTimeType type;
    private Optional<Cron> asCron;
    private Optional<WhenTime> asWhenTime;
    private Optional<ClockTime> asClockTime;

    /**
     * Creates a DelayedTimeValue from a Cron. This is a special Cosntructor that takes a DelayedTimeType
     * because there are multiple types of cron that we support
     *
     * @param type
     *  The DelayedTimeType should be a type of a cron
     * @param cron
     *  The underlying Cron instance
     * @throws IllegalArgumentException
     *  Throws an IllegalArgumentException if the DelayedTimeType isn't a possible Cron type
     */
    public DelayedTimeValue(DelayedTimeType type, Cron cron) throws IllegalArgumentException {
        switch (type) {
            case CRON4J_CRON:
            case QUARTZ_CRON:
            case UNIX_CRON:
                this.type = type;
                this.asCron = Optional.of(cron);
                this.asWhenTime = Optional.empty();
                this.asClockTime = Optional.empty();
                break;
            default:
                throw new IllegalArgumentException("Invalid Delayed Time Type.");
        }
    }

    /**
     * Creates a DelayedTimeValue from a "WhenTime" instance
     *
     * @param whenTime
     *  The underlying whenTime
     */
    public DelayedTimeValue(WhenTime whenTime) {
        this.type = DelayedTimeType.WHEN_TIME;
        this.asWhenTime = Optional.of(whenTime);
        this.asCron = Optional.empty();
        this.asClockTime = Optional.empty();
    }

    /**
     * Creates a DelayedTimeValue from a "ClockTime" instance
     *
     * @param clockTime
     *  The underlying ClockTime
     */
    public DelayedTimeValue(ClockTime clockTime) {
        this.type = DelayedTimeType.CLOCK_TIME;
        this.asClockTime = Optional.of(clockTime);
        this.asWhenTime = Optional.empty();
        this.asCron = Optional.empty();
    }

    /**
     * @return
     *  The underlying Cron instance regardless of the actual type of delayed time value
     */
    public Cron getAsCron() {
        switch(this.type) {
            case CRON4J_CRON:
            case QUARTZ_CRON:
            case UNIX_CRON:
                return this.asCron.get();
            case WHEN_TIME:
                return this.asWhenTime.get().getBackingCronInstance();
            case CLOCK_TIME:
                return this.asClockTime.get().getBackingCron();
            default:
                return null;
        }
    }

    /**
     * @return
     *  If the user specified to only run this once
     */
    public boolean isSingle() {
        if (this.type == DelayedTimeType.WHEN_TIME) {
            return this.asWhenTime.get().isSingle();
        }
        if (this.type == DelayedTimeType.CLOCK_TIME) {
            return this.asClockTime.get().isSingle();
        }
        return false;
    }

    /**
     * @return
     *  The potential Underlying Cron instance
     */
    public Optional<Cron> getUnderlyingCron() {
        return this.asCron;
    }

    /**
     * @return
     *  The potential Underlying WhenTime instance
     */
    public Optional<WhenTime> getUnderlyingWhenTime() {
        return this.asWhenTime;
    }

    /**
     * @return
     *  The potential Underlying ClockTime instance
     */
    public Optional<ClockTime> getUnderlyingClockTime() {
        return this.asClockTime;
    }

    /**
     * @return
     *  The DelayedTime as a String
     */
    public String asString() {
        switch (this.type) {
            case CRON4J_CRON:
            case QUARTZ_CRON:
            case UNIX_CRON:
                return this.asCron.get().asString();
            case WHEN_TIME:
                return this.asWhenTime.get().asString();
            case CLOCK_TIME:
                return this.asClockTime.get().asString();
            default:
                return null;
        }
    }
}
