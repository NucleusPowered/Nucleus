/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mute.services;

import io.github.nucleuspowered.nucleus.api.module.mute.data.Mute;
import io.github.nucleuspowered.nucleus.api.util.data.TimedEntry;
import io.github.nucleuspowered.nucleus.core.datatypes.NucleusTimedEntry;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.persistence.Queries;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public final class NucleusMute implements Mute {

    private static final int CONTENT_VERSION = 1;

    private static final DataQuery REASON = DataQuery.of("reason");
    private static final DataQuery MUTER = DataQuery.of("muter");
    private static final DataQuery CREATION_INSTANT = DataQuery.of("creationInstant");

    public NucleusMute(final NucleusMute copy, final @Nullable TimedEntry start) {
        this.timedEntry = start;
        this.reason = copy.reason;
        this.creationInstant = copy.creationInstant;
        this.muter = copy.muter;
        this.isSavedStopped = copy.isSavedStopped;
    }

    public static NucleusMute fromMutingRequest(final UUID user,
                                                final String reason,
                                                @Nullable final UUID jailer,
                                                @Nullable final Instant creationInstant,
                                                @Nullable final Duration timeRemaining,
                                                final BooleanSupplier isSavedStopped) {
        final TimedEntry timedEntry;
        if (timeRemaining == null) {
            timedEntry = null;
        } else if (!Sponge.server().player(user).isPresent() && isSavedStopped.getAsBoolean()) {
            timedEntry = new NucleusTimedEntry.Stopped(timeRemaining);
        } else {
            timedEntry = new NucleusTimedEntry.Ticking(Instant.now().plus(timeRemaining));
        }

        return new NucleusMute(reason, jailer, creationInstant, timedEntry, isSavedStopped);
    }

    final BooleanSupplier isSavedStopped;
    final String reason;
    @Nullable final UUID muter;
    @Nullable final Instant creationInstant;
    @Nullable final TimedEntry timedEntry;

    public NucleusMute(final String reason, @Nullable final UUID muter, @Nullable final Instant creationInstant, final @Nullable TimedEntry entry, final BooleanSupplier isSavedStopped) {
        this.reason = reason;
        this.muter = muter;
        this.creationInstant = creationInstant;
        this.timedEntry = entry;
        this.isSavedStopped = isSavedStopped;
    }

    @Override
    public String getReason() {
        return this.reason;
    }

    @Override
    public Optional<UUID> getMuter() {
        return Optional.ofNullable(this.muter);
    }

    @Override
    public Optional<Instant> getCreationInstant() {
        return Optional.ofNullable(this.creationInstant);
    }

    @Override
    public Optional<TimedEntry> getTimedEntry() {
        return Optional.ofNullable(this.timedEntry);
    }

    @Override
    public int contentVersion() {
        return NucleusMute.CONTENT_VERSION;
    }

    @Override
    public DataContainer toContainer() {
        final DataContainer container = DataContainer.createNew()
                .set(Queries.CONTENT_VERSION, NucleusMute.CONTENT_VERSION)
                .set(NucleusMute.MUTER, this.muter)
                .set(NucleusMute.REASON, this.reason);
        if (this.creationInstant != null) {
            container.set(NucleusMute.CREATION_INSTANT, this.creationInstant.getEpochSecond());
        }
        this.getTimedEntry().ifPresent(x -> {
            if (this.isSavedStopped.getAsBoolean()) {
                container.set(NucleusTimedEntry.TIMED_ENTRY, x.stop());
            } else {
                container.set(NucleusTimedEntry.TIMED_ENTRY, x.start());
            }
        });
        return container;
    }

    public Mute start() {
        return this.getTimedEntry().filter(x -> !x.isCurrentlyTicking()).map(x -> new NucleusMute(this, x.start())).orElse(this);
    }

    public Mute stop() {
        return this.getTimedEntry().filter(TimedEntry::isCurrentlyTicking).map(x -> new NucleusMute(this, x.stop())).orElse(this);
    }

    public static final class DataBuilder extends AbstractDataBuilder<Mute> {

        private final BooleanSupplier isSavedStopped;

        public DataBuilder(final BooleanSupplier isSavedStopped) {
            super(Mute.class, NucleusMute.CONTENT_VERSION);
            this.isSavedStopped = isSavedStopped;
        }


        @Override
        protected Optional<Mute> buildContent(final DataView container) throws InvalidDataException {
            if (!container.contains(Queries.CONTENT_VERSION)) {
                NucleusTimedEntry.upgradeLegacy(container, NucleusTimedEntry.TIMED_ENTRY);
            }

            return Optional.of(
                    new NucleusMute(
                            container.getString(NucleusMute.REASON).orElse(null),
                            container.getObject(NucleusMute.MUTER, UUID.class).orElse(null),
                            container.getLong(NucleusMute.CREATION_INSTANT).map(Instant::ofEpochSecond).orElse(null),
                            container.getObject(NucleusTimedEntry.TIMED_ENTRY, NucleusTimedEntry.class).orElse(null),
                            this.isSavedStopped
                    )
            );
        }
    }

}
