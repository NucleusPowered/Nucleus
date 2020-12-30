/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.module.warning.data;

import io.github.nucleuspowered.nucleus.api.util.data.TimedEntry;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Information about a player's warning.
 *
 * <p>Note that warnings are no longer part of the Nucleus plugin,
 * this exists for migration purposes.</p>
 */
@Deprecated
public interface Warning extends TimedEntry {

    /**
     * The {@link UUID} of the player who issued the warning, or {@link Optional#empty()} if it was the console.
     *
     * @return The UUID of the warning player.
     */
    Optional<UUID> getWarner();

    /**
     * When the warning was issued.
     *
     * @return The {@link Instant} the warning was issued.
     */
    Instant getDate();

    /**
     * The reason for the warning.
     *
     * @return The reason.
     */
    String getReason();

    /**
     * Whether the warning has expired.
     *
     * @return <code>true</code> if so.
     */
    boolean isExpired();
}
