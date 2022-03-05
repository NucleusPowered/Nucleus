/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.module.mute.data;

import io.github.nucleuspowered.nucleus.api.util.data.TimedEntry;
import org.spongepowered.api.data.persistence.DataSerializable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Provides information about a player's mute.
 */
public interface Mute extends DataSerializable {

    /**
     * The reason for the mute.
     * @return The reason.
     */
    String getReason();

    /**
     * The {@link UUID} of the muter, or {@link Optional#empty()} if the muter was not a player.
     * @return The {@link UUID} of the muter, if applicable.
     */
    Optional<UUID> getMuter();

    /**
     * Gets the {@link Instant} this player was muted, if this information was recorded.
     *
     * @since 0.27
     *
     * @return The instant, if known.
     */
    Optional<Instant> getCreationInstant();

    /**
     * Gets the {@link TimedEntry} which represents how long this mute has to go.
     *
     * @return The timed entry.
     */
    Optional<TimedEntry> getTimedEntry();

}
