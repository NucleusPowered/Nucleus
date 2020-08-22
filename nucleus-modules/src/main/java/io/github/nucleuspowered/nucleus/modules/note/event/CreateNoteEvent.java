/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note.event;

import io.github.nucleuspowered.nucleus.api.module.note.event.NucleusNoteEvent;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.plugin.meta.util.NonnullByDefault;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

@NonnullByDefault
public class CreateNoteEvent implements NucleusNoteEvent.Created {

    @Nullable private final UUID author;
    private final String note;
    private final User targetUser;
    private final Cause cause;
    private final Instant instant;

    public CreateNoteEvent(@Nullable UUID author, String note, Instant date, User targetUser, Cause cause) {
        this.author = author;
        this.note = note;
        this.instant = date;
        this.targetUser = targetUser;
        this.cause = cause;
    }

    @Override
    public Optional<UUID> getAuthor() {
        return Optional.ofNullable(this.author);
    }

    @Override
    public Instant getDate() {
        return this.instant;
    }

    @Override
    public String getNote() {
        return this.note;
    }

    @Override
    public User getTargetUser() {
        return this.targetUser;
    }

    @Override
    public Cause getCause() {
        return this.cause;
    }
}
