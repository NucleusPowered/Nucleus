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
 * A Variable Time Format. An alternative to Cron syntax that can be expressed like:
 * </p>
 *
 * <pre>
 * every 15 minutes
 * every hour
 * </pre>
 *
 * <p>
 * This is a fairly simple syntax loosely based off the Whenjobs time parsing format.
 * Internally the WhenTime is backed by a Cron so we can have one Single handling case for delayed
 * tasks, and don't have to jump through hoops.
 * </p>
 */
public class WhenTime {

    private QuartzTimeUnit timeUnit;
    private int every;
    private boolean isSingle;
    private Cron backingCronInstance;

    /**
     * Construct a Variable Time
     *
     * @param timeUnit
     *  The TimeUnit of the variable. (It should be noted Days will be treated as: Days of the Week)
     * @param every
     *  The value of TimeUnits to wait. E.g. in "every 2 days" 2 would be the "every"
     * @throws IllegalArgumentException
     *  Throws an IllegalArgumentException if the TimeUnit is too granular/non-existant for a cron, and thus can't
     *  be parsed
     */
    public WhenTime(QuartzTimeUnit timeUnit, int every) throws IllegalArgumentException {
        this.timeUnit = timeUnit;
        this.every = every;
        CronBuilder cronBuilder = CronBuilder.cron(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
        switch (timeUnit) {
            case DAY_OF_WEEK:
                cronBuilder.withDoW(FieldExpressionFactory.every(this.every))
                        .withDoM(FieldExpressionFactory.questionMark())
                        .withYear(FieldExpressionFactory.always())
                        .withMonth(FieldExpressionFactory.always())
                        .withHour(FieldExpressionFactory.on(0))
                        .withMinute(FieldExpressionFactory.on(0))
                        .withSecond(FieldExpressionFactory.on(0));
                break;
            case DAY_OF_MONTH:
                cronBuilder.withDoM(FieldExpressionFactory.every(this.every))
                        .withDoW(FieldExpressionFactory.questionMark())
                        .withYear(FieldExpressionFactory.always())
                        .withMonth(FieldExpressionFactory.always())
                        .withHour(FieldExpressionFactory.on(0))
                        .withMinute(FieldExpressionFactory.on(0))
                        .withSecond(FieldExpressionFactory.on(0));
                break;
            case MONTHS:
                cronBuilder.withDoM(FieldExpressionFactory.on(1))
                        .withDoW(FieldExpressionFactory.questionMark())
                        .withYear(FieldExpressionFactory.always())
                        .withMonth(FieldExpressionFactory.every(this.every))
                        .withHour(FieldExpressionFactory.on(0))
                        .withMinute(FieldExpressionFactory.on(0))
                        .withSecond(FieldExpressionFactory.on(0));
                break;
            case HOURS:
                cronBuilder.withDoM(FieldExpressionFactory.always())
                        .withDoW(FieldExpressionFactory.questionMark())
                        .withYear(FieldExpressionFactory.always())
                        .withMonth(FieldExpressionFactory.always())
                        .withHour(FieldExpressionFactory.every(this.every))
                        .withMinute(FieldExpressionFactory.on(0))
                        .withSecond(FieldExpressionFactory.on(0));
                break;
            case MINUTES:
                cronBuilder.withDoM(FieldExpressionFactory.always())
                        .withDoW(FieldExpressionFactory.questionMark())
                        .withYear(FieldExpressionFactory.always())
                        .withMonth(FieldExpressionFactory.always())
                        .withHour(FieldExpressionFactory.on(0))
                        .withMinute(FieldExpressionFactory.every(this.every))
                        .withSecond(FieldExpressionFactory.on(0));
                break;
            case SECONDS:
                cronBuilder.withDoM(FieldExpressionFactory.always())
                        .withDoW(FieldExpressionFactory.questionMark())
                        .withYear(FieldExpressionFactory.always())
                        .withMonth(FieldExpressionFactory.always())
                        .withHour(FieldExpressionFactory.on(0))
                        .withMinute(FieldExpressionFactory.on(0))
                        .withSecond(FieldExpressionFactory.every(this.every));
                break;
            default:
                throw new IllegalArgumentException("Time Unit is not supported by Quartz CRON.");
        }
        this.isSingle = false;
        this.backingCronInstance = cronBuilder.instance();
    }

    public WhenTime(QuartzTimeUnit timeUnit, int every, boolean isSingle) throws IllegalArgumentException {
        this(timeUnit, every);
        this.isSingle = isSingle;
    }

    /**
     * @return
     *  If the user specified to only run this once
     */
    public boolean isSingle() {
        return this.isSingle;
    }

    /**
     * Grab the backing cron instance for this WhenTime
     *
     * @return
     *  The backing cron instance
     */
    public Cron getBackingCronInstance() {
        return this.backingCronInstance;
    }

    /**
     * Converts the WhenTime to a String
     *
     * @return
     *  The WhenTime as a String
     */
    public String asString() {
        if (!this.isSingle) {
            if (this.every == 1) {
                return String.format("every %s", this.timeUnit.name().toLowerCase());
            } else {
                return String.format("every %d %s", this.every, this.timeUnit.name().toLowerCase());
            }
        } else {
            if (this.every == 1) {
                return String.format("in %s", this.timeUnit.name().toLowerCase());
            } else {
                return String.format("in %d %s", this.every, this.timeUnit.name().toLowerCase());
            }
        }
    }

    /**
     * Parses a WhenTime from a String
     *
     * @param string
     *  The String to parse
     * @return
     *  The WhenTime represented by the String
     * @throws IllegalArgumentException
     *  Throws an IllegalArgumentException if we could not parse the string
     * @throws NullPointerException
     *  Throws a NullPointerException if the String is null
     */
    public static WhenTime fromString(String string) throws IllegalArgumentException, NullPointerException {
        Preconditions.checkNotNull(string);
        if (!string.contains(" ")) {
            throw new IllegalArgumentException("Expression doesn't have the split character.");
        }
        String[] split = string.split(" ");
        if (split.length > 3 || split.length < 2) {
            throw new IllegalArgumentException("Expression doesn't has to many directives.");
        }
        if (!split[0].equalsIgnoreCase("every") && !split[0].equalsIgnoreCase("in")) {
            throw new IllegalArgumentException("Unknown Starting Expression");
        }
        String possibleTimeUnit = split[split.length - 1];
        QuartzTimeUnit timeUnit = QuartzTimeUnit.fromString(possibleTimeUnit);
        if (timeUnit == null) {
            throw new IllegalArgumentException("Expression doesn't have a valid time unit.");
        }
        int every = 1;
        if (split.length == 3) {
            String toParseTime = split[1];
            try {
                every = Integer.parseInt(toParseTime);
            } catch (Exception e1) {
                throw new IllegalArgumentException("Expression doesn't have a valid time for every.");
            }
        }
        return new WhenTime(timeUnit, every, split[0].equalsIgnoreCase("in"));
    }
}
