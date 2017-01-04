/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.util;

import javax.annotation.Nullable;

/**
 * A Type of Delayed Time. E.g. is the timing a cron, HH:MM, or?
 */
public enum DelayedTimeType {
    UNIX_CRON,
    QUARTZ_CRON,
    CRON4J_CRON,
    WHEN_TIME,
    CLOCK_TIME;

    @Nullable
    public static DelayedTimeType fromString(String value) {
        if (value.equalsIgnoreCase("unix_cron") || value.equalsIgnoreCase("unix-cron") ||
                value.equalsIgnoreCase("unix")) {
            return DelayedTimeType.UNIX_CRON;
        }
        if (value.equalsIgnoreCase("quartz_cron") || value.equalsIgnoreCase("quartz-cron") ||
                value.equalsIgnoreCase("quartz")) {
            return DelayedTimeType.QUARTZ_CRON;
        }
        if (value.equalsIgnoreCase("cron4j_cron") || value.equalsIgnoreCase("cron4j-cron") ||
                value.equalsIgnoreCase("cron4j")) {
            return DelayedTimeType.CRON4J_CRON;
        }
        if (value.equalsIgnoreCase("when_time") || value.equalsIgnoreCase("when-time") ||
                value.equalsIgnoreCase("when")) {
            return DelayedTimeType.WHEN_TIME;
        }
        if (value.equalsIgnoreCase("clock_time") || value.equalsIgnoreCase("clock-time") ||
                value.equalsIgnoreCase("clock")) {
            return DelayedTimeType.CLOCK_TIME;
        }
        return null;
    }
}
