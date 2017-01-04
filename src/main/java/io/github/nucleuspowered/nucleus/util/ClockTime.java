/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.util;

import com.cronutils.builder.CronBuilder;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.expression.FieldExpressionFactory;
import com.cronutils.utils.Preconditions;

/**
 * <p>
 * A Variable Time Format. An alternative syntax to cron that can be expressed like:
 * </p>
 *
 * <pre>
 * 10:30
 * 10:30:30
 * </pre>
 *
 * <p>
 * This is fairly simple time format, and automatically assumes every year/month/day.
 * Internally ClockTime is backed by a Cron so we can have one Single handling case for delayed
 * tasks, and don't have to jump through hoops.
 * </p>
 */
public class ClockTime {

    private int hours;
    private int minutes;
    private int seconds;
    private boolean isSingle;
    private Cron backingCron;

    /**
     * Constructs a clock time that has been formatted as: `HH:MM`
     *
     * @param hours
     *  The Hours
     * @param minutes
     *  The Minutes
     */
    public ClockTime(int hours, int minutes) {
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = -1;
        CronBuilder cronBuilder = CronBuilder.cron(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
        cronBuilder.withYear(FieldExpressionFactory.always())
                .withDoM(FieldExpressionFactory.always())
                .withDoW(FieldExpressionFactory.questionMark())
                .withMonth(FieldExpressionFactory.always())
                .withHour(FieldExpressionFactory.on(this.hours))
                .withMinute(FieldExpressionFactory.on(this.minutes))
                .withSecond(FieldExpressionFactory.on(0));
        this.backingCron = cronBuilder.instance();
        this.isSingle = false;
    }

    /**
     * Constructs a clock time that has been formatted as: `HH:MM:SS`
     *
     * @param hours
     *  The Hours
     * @param minutes
     *  The Minutes
     * @param seconds
     *  The Seconds
     */
    public ClockTime(int hours, int minutes, int seconds) {
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
        CronBuilder cronBuilder = CronBuilder.cron(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
        cronBuilder.withYear(FieldExpressionFactory.always())
                .withDoM(FieldExpressionFactory.always())
                .withDoW(FieldExpressionFactory.questionMark())
                .withMonth(FieldExpressionFactory.always())
                .withHour(FieldExpressionFactory.on(this.hours))
                .withMinute(FieldExpressionFactory.on(this.minutes))
                .withSecond(FieldExpressionFactory.on(this.seconds));
        this.backingCron = cronBuilder.instance();
        this.isSingle = false;
    }

    /**
     * Constructs a clock time in `HH:MM` while manually specifying isSingle
     *
     * @param hours
     *  The hours
     * @param minutes
     *  The minutes
     * @param isSingle
     *  If this job should only be run once
     */
    public ClockTime(int hours, int minutes, boolean isSingle) {
        this(hours, minutes);
        this.isSingle = isSingle;
    }

    /**
     * Constructs a clock time in `HH:MM:SS` while manually specifying isSingle
     *
     * @param hours
     *  The hours
     * @param minutes
     *  The minutes
     * @param seconds
     *  The seconds
     * @param isSingle
     *  If this job should only be run once
     */
    public ClockTime(int hours, int minutes, int seconds, boolean isSingle) {
        this(hours, minutes, seconds);
        this.isSingle = isSingle;
    }

    /**
     * @return
     *  Whether the user only wanted this to run once
     */
    public boolean isSingle() {
        return this.isSingle;
    }

    /**
     * @return
     *  The backing cron instance
     */
    public Cron getBackingCron() {
        return this.backingCron;
    }

    /**
     * Convert this ClockTime to a String
     *
     * @return
     *  The ClockTime as a String
     */
    public String asString() {
        if (!this.isSingle) {
            if (this.seconds == -1) {
                return String.format("%02d:%02d", this.hours, this.minutes);
            } else {
                return String.format("%02d:%02d:%02d", this.hours, this.minutes, this.seconds);
            }
        } else {
            if (this.seconds == -1) {
                return String.format("at %02d:%02d", this.hours, this.minutes);
            } else {
                return String.format("at %02d:%02d:%02d", this.hours, this.minutes, this.seconds);
            }
        }
    }

    /**
     * Create a ClockTime from a String
     *
     * @param string
     *  The string to create the ClockTime from
     * @return
     *  The ClockTime
     * @throws NullPointerException
     *  Throws a NullPointerException if the string is null
     * @throws IllegalArgumentException
     *  Throws an IllegalArgumentException if we can't parse the ClockTime
     */
    public static ClockTime fromString(String string) throws NullPointerException, IllegalArgumentException {
        Preconditions.checkNotNull(string);
        String stringFrd;
        boolean isSingle = false;
        if (string.contains(" ")) {
            isSingle = true;
            String[] splitFirst = string.split(" ");
            if (splitFirst.length != 2) {
                throw new IllegalArgumentException("Expression has too many spaces!");
            }
            if (!splitFirst[0].equalsIgnoreCase("at")) {
                throw new IllegalArgumentException("Expression has unknown starting directive.");
            }
            stringFrd = splitFirst[1];
        } else {
            stringFrd = string;
        }
        if (!stringFrd.contains(":")) {
            throw new IllegalArgumentException("Expression doesn't have the split character.");
        }
        String[] split = stringFrd.split(":");
        if (split.length > 3 || split.length < 2) {
            throw new IllegalArgumentException("Expression doesn't have the right amount of numbers.");
        }
        try {
            int hours = Integer.parseInt(split[0]);
            int minutes = Integer.parseInt(split[1]);
            if (split.length == 3) {
                int seconds = Integer.parseInt(split[2]);
                return new ClockTime(hours, minutes, seconds, isSingle);
            } else {
                return new ClockTime(hours, minutes, isSingle);
            }
        } catch (Exception e1) {
            throw new IllegalArgumentException(e1.getCause());
        }
    }
}
