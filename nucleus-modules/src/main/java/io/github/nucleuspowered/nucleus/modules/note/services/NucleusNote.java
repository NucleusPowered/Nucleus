/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note.services;

import io.github.nucleuspowered.nucleus.api.module.note.data.Note;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class NucleusNote implements Note {

    private static final int CONTENT_VERSION = 1;

    private static final DataQuery NOTER = DataQuery.of("noter");
    private static final DataQuery NOTE = DataQuery.of("note");
    private static final DataQuery DATE = DataQuery.of("date");

    private final @Nullable UUID noter;
    private final String note;
    private final Instant date;

    public NucleusNote(final @Nullable UUID noter, final String note, final Instant date) {
        this.noter = noter;
        this.note = note;
        this.date = date;
    }

    @Override
    public String getNote() {
        return this.note;
    }

    @Override
    public Optional<UUID> getNoter() {
        return Optional.ofNullable(this.noter);
    }

    @Override
    public Instant getDate() {
        return this.date;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final NucleusNote nucleusNote = (NucleusNote) o;
        return Objects.equals(this.noter, nucleusNote.noter) &&
                Objects.equals(this.note, nucleusNote.note) &&
                Objects.equals(this.date, nucleusNote.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.noter, this.note, this.date);
    }

    @Override
    public int contentVersion() {
        return NucleusNote.CONTENT_VERSION;
    }

    @Override
    public DataContainer toContainer() {
        final DataContainer dataContainer = DataContainer.createNew();
        if (this.noter != null) {
            dataContainer.set(NucleusNote.NOTER, this.noter);
        }
        return dataContainer
                .set(NucleusNote.NOTE, this.note)
                .set(NucleusNote.DATE, this.date.toEpochMilli());
    }

    public static final class DataBuilder extends AbstractDataBuilder<Note> {

        public DataBuilder() {
            super(Note.class, NucleusNote.CONTENT_VERSION);
        }

        @Override
        protected Optional<Note> buildContent(final DataView container) throws InvalidDataException {
            if (container.contains(NucleusNote.NOTE, NucleusNote.DATE)) {
                return Optional.of(new NucleusNote(
                        container.getObject(NucleusNote.NOTER, UUID.class).orElse(null),
                        container.getString(NucleusNote.NOTE).get(),
                        container.getLong(NucleusNote.DATE).map(Instant::ofEpochMilli).get()
                ));
            }
            return Optional.empty();
        }
    }

}
