/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note.data;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.module.note.data.Note;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ConfigSerializable
public class NoteData implements Note {
    @Setting
    private UUID noter;

    @Setting
    private String note;

    @Setting
    private long date;

    public NoteData() { }

    public NoteData(Instant date, UUID noter, String note) {
        this.noter = noter;
        this.note = note;
        this.date = date.toEpochMilli();
    }

    @Override public String getNote() {
        return this.note;
    }

    @Override public Optional<UUID> getNoter() {
        return this.noter.equals(Util.CONSOLE_FAKE_UUID) ? Optional.empty() : Optional.of(this.noter);
    }

    public UUID getNoterInternal() {
        return this.noter;
    }

    @Override public Instant getDate() {
        return Instant.ofEpochMilli(this.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNoter(), getNote(), getDate());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NoteData)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!this.getNote().equals(((NoteData) o).getNote())) {
            return false;
        }
        if (!this.getDate().equals(((NoteData) o).getDate())) {
            return false;
        }
        return this.getNoterInternal().equals(((NoteData) o).getNoter().orElse(Util.CONSOLE_FAKE_UUID));
    }

}
