/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.datatypes;

import io.github.nucleuspowered.nucleus.api.util.data.TimedEntry;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.DataBuilder;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.persistence.Queries;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public abstract class NucleusTimedEntry implements TimedEntry {

    public static final int CONTENT_VERSION = 1;

    public static final DataQuery TIMED_ENTRY = DataQuery.of("timedEntry");
    private static final DataQuery REMAINING_SECONDS = DataQuery.of("remainingSeconds");
    private static final DataQuery ABSOLUTE_TIME = DataQuery.of("absoluteTime");

    @Override
    public boolean isCurrentlyTicking() {
        return false;
    }

    @Override
    public int contentVersion() {
        return NucleusTimedEntry.CONTENT_VERSION;
    }

    public static class Ticking extends NucleusTimedEntry {

        private final Instant targetInstant;

        public Ticking(final Instant targetInstant) {
            this.targetInstant = targetInstant;
        }

        @Override
        public Duration getRemainingTime() {
            return Duration.between(Instant.now(), this.targetInstant);
        }

        @Override
        public boolean expired() {
            return this.getRemainingTime().getSeconds() <= 0;
        }

        @Override
        public boolean isCurrentlyTicking() {
            return true;
        }

        @Override
        public TimedEntry start() {
            return this;
        }

        @Override
        public TimedEntry stop() {
            return new NucleusTimedEntry.Stopped(this.getRemainingTime());
        }

        @Override
        public DataContainer toContainer() {
            return DataContainer.createNew().set(NucleusTimedEntry.ABSOLUTE_TIME, this.targetInstant.getEpochSecond());
        }
    }

    public static class Stopped extends NucleusTimedEntry {

        private final Duration duration;

        public Stopped(final Duration duration) {
            this.duration = duration;
        }

        @Override
        public Duration getRemainingTime() {
            return this.duration;
        }

        @Override
        public boolean isCurrentlyTicking() {
            return false;
        }

        @Override
        public TimedEntry start() {
            return new NucleusTimedEntry.Ticking(Instant.now().plus(this.duration));
        }

        @Override
        public TimedEntry stop() {
            return this;
        }

        @Override
        public boolean expired() {
            return false;
        }

        @Override
        public DataContainer toContainer() {
            return DataContainer.createNew().set(NucleusTimedEntry.REMAINING_SECONDS, this.getRemainingTime());
        }
    }

    public static DataView upgradeLegacy(final DataView incoming, final DataQuery child) {
        final DataView view = child.queryParts().isEmpty() ? incoming : incoming.createView(child);
        incoming.getLong(DataQuery.of("absoluteTime")).ifPresent(x -> view.set(NucleusTimedEntry.ABSOLUTE_TIME, x));
        incoming.getLong(DataQuery.of("timeFromNextLogin")).ifPresent(x -> view.set(NucleusTimedEntry.REMAINING_SECONDS, x));
        return incoming.remove(DataQuery.of("absoluteTime")).remove(DataQuery.of("timeFromNextLogin"));
    }

    public static class DataBuilder extends AbstractDataBuilder<TimedEntry> {

        public DataBuilder() {
            super(TimedEntry.class, NucleusTimedEntry.CONTENT_VERSION);
        }

        @Override
        protected Optional<TimedEntry> buildContent(final DataView container) throws InvalidDataException {
            if (!container.contains(Queries.CONTENT_VERSION)) {
                upgradeLegacy(container, DataQuery.of());
            }
            if (container.contains(NucleusTimedEntry.REMAINING_SECONDS)) {
                return Optional.of(new NucleusTimedEntry.Ticking(Instant.ofEpochSecond(container.getLong(NucleusTimedEntry.REMAINING_SECONDS).get())));
            }
            if (container.contains(NucleusTimedEntry.ABSOLUTE_TIME)) {
                return Optional.of(new NucleusTimedEntry.Stopped(Duration.ofSeconds(container.getLong(NucleusTimedEntry.ABSOLUTE_TIME).get())));
            }
            return Optional.empty();
        }
    }



}
