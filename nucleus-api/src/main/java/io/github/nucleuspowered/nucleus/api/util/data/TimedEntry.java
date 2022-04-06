/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.util.data;

import org.spongepowered.api.data.persistence.DataSerializable;

import java.time.Duration;
import java.util.Optional;

/**
 * Indicates that there might be a time limit on this entry.
 */
public interface TimedEntry extends DataSerializable {

    /**
     * The amount of time remaining before this entry expires, if applicable.
     *
     * @return The remaining amount of time.
     */
    Duration getRemainingTime();

    /**
     * Returns whether this entry can be considered expired.
     *
     * @return if expired.
     */
    boolean expired();

    /**
     * Denotes whether the timer is currently ticking down (that is, if {@link #getRemainingTime()} should be decreasing with
     * each call.
     *
     * @return <code>true</code> if so.
     */
    boolean isCurrentlyTicking();

    /**
     * Returns a {@link TimedEntry} that is currently ticking.
     *
     * @return An entry that represents something that's ticking down.
     */
    TimedEntry start();

    /**
     * Returns a {@link TimedEntry} that is currently not ticking.
     *
     * @return An entry that represents something that is not ticking down.
     */
    TimedEntry stop();
}
