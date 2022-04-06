/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.module.note.data;

import org.spongepowered.api.data.persistence.DataSerializable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a note on a user's account.
 */
public interface Note extends DataSerializable {

    /**
     * The contents of the note.
     *
     * @return The note.
     */
    String getNote();

    /**
     * The {@link UUID} of the player who added the note, or {@link Optional#empty()} if this was the console.
     *
     * @return The UUID
     */
    Optional<UUID> getNoter();

    /**
     * The {@link Instant} the note was placed.
     *
     * @return The {@link Instant}
     */
    Instant getDate();
}
