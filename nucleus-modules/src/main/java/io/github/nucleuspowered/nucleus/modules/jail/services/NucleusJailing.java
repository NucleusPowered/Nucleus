/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.services;

import io.github.nucleuspowered.nucleus.api.module.jail.data.Jailing;
import io.github.nucleuspowered.nucleus.api.util.data.TimedEntry;
import io.github.nucleuspowered.nucleus.core.datatypes.NucleusTimedEntry;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.persistence.Queries;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.math.vector.Vector3d;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public class NucleusJailing implements Jailing {

    public static final int CONTENT_VERSION = 1;

    private static final DataQuery REASON = DataQuery.of("reason");
    private static final DataQuery JAIL_NAME = DataQuery.of("jailName");
    private static final DataQuery JAILER = DataQuery.of("jailer");
    private static final DataQuery PREVIOUS_LOCATION_WORLD = DataQuery.of("previousLocation", "world");
    private static final DataQuery PREVIOUS_LOCATION_POSITION = DataQuery.of("previousLocation", "position");
    private static final DataQuery CREATION_INSTANT = DataQuery.of("creationInstant");

    public static NucleusJailing fromJailingRequest(final String reason, final String jailName,
                                                    @Nullable final UUID jailer, @Nullable final ServerLocation previousLocation,
                                                    @Nullable final Instant creationInstant, @Nullable final TimedEntry timedEntry,
                                                    final BooleanSupplier shouldSaveStopped) {
        return new NucleusJailing(reason, jailName, jailer, previousLocation, creationInstant, timedEntry, shouldSaveStopped);
    }

    final BooleanSupplier shouldSaveStopped;
    final String reason;
    final String jailName;
    @Nullable final UUID jailer;
    @Nullable final ServerLocation previousLocation;
    @Nullable final Instant creationInstant;
    @Nullable final TimedEntry expiryTime;

    public NucleusJailing(final NucleusJailing copy, @Nullable final TimedEntry timedEntry) {
        this.reason = copy.reason;
        this.jailName = copy.jailName;
        this.jailer = copy.jailer;
        this.previousLocation = copy.previousLocation;
        this.creationInstant = copy.creationInstant;
        this.shouldSaveStopped = copy.shouldSaveStopped;
        this.expiryTime = timedEntry;
    }

    public NucleusJailing(final String reason, final String jailName, @Nullable final UUID jailer,
                             @Nullable final ServerLocation previousLocation, @Nullable final Instant creationInstant,
                             @Nullable final TimedEntry timedEntry, final BooleanSupplier shouldSaveStopped) {
        this.reason = reason;
        this.jailName = jailName;
        this.jailer = jailer;
        this.previousLocation = previousLocation;
        this.creationInstant = creationInstant;
        this.expiryTime = timedEntry;
        this.shouldSaveStopped = shouldSaveStopped;
    }

    @Override
    public String getReason() {
        return this.reason;
    }

    @Override
    public String getJailName() {
        return this.jailName;
    }

    @Override
    public Optional<UUID> getJailer() {
        return Optional.ofNullable(this.jailer);
    }

    @Override
    public Optional<ServerLocation> getPreviousLocation() {
        return Optional.ofNullable(this.previousLocation);
    }

    @Override
    public Optional<Instant> getCreationInstant() {
        return Optional.ofNullable(this.creationInstant);
    }

    @Override
    public Optional<TimedEntry> getTimedEntry() {
        return Optional.ofNullable(this.expiryTime);
    }

    @Override
    public int contentVersion() {
        return NucleusJailing.CONTENT_VERSION;
    }

    public Jailing start() {
        if (this.expiryTime != null && !this.expiryTime.isCurrentlyTicking()) {
            return new NucleusJailing(this, this.expiryTime.start());
        } else {
            return this;
        }
    }

    public Jailing stop() {
        if (this.expiryTime != null && this.expiryTime.isCurrentlyTicking()) {
            return new NucleusJailing(this, this.expiryTime.stop());
        } else {
            return this;
        }
    }

    public DataContainer toContainer() {
        final DataContainer container = DataContainer.createNew()
                .set(Queries.CONTENT_VERSION, NucleusJailing.CONTENT_VERSION)
                .set(NucleusJailing.REASON, this.reason)
                .set(NucleusJailing.JAIL_NAME, this.jailName)
                .set(NucleusJailing.JAILER, this.jailer);
        this.getPreviousLocation().ifPresent(x -> {
            container.set(NucleusJailing.PREVIOUS_LOCATION_POSITION, x.position());
            container.set(NucleusJailing.PREVIOUS_LOCATION_WORLD, x.worldKey());
        });
        this.getCreationInstant().ifPresent(x -> container.set(NucleusJailing.CREATION_INSTANT, x.getEpochSecond()));
        this.getTimedEntry().ifPresent(x -> container.set(NucleusTimedEntry.TIMED_ENTRY, this.shouldSaveStopped.getAsBoolean() ? x.stop() : x.start()));
        return container;
    }

    public static final class DataBuilder extends AbstractDataBuilder<Jailing> {

        private static final Vector3d DEFAULT = Vector3d.ZERO.min(0d, -1d, 0d);

        private final BooleanSupplier shouldSaveStopped;

        public DataBuilder(final BooleanSupplier shouldSaveStopped) {
            super(Jailing.class, NucleusJailing.CONTENT_VERSION);
            this.shouldSaveStopped = shouldSaveStopped;
        }

        @Override
        protected Optional<Jailing> buildContent(final DataView container) throws InvalidDataException {
            if (!container.contains(Queries.CONTENT_VERSION)) {
                NucleusTimedEntry.upgradeLegacy(container, NucleusTimedEntry.TIMED_ENTRY);

                final @Nullable ResourceKey previousWorld = container.getObject(DataQuery.of("world"), UUID.class)
                        .flatMap(x -> Sponge.server().worldManager().worldKey(x))
                        .orElse(null);
                if (previousWorld != null) {
                    final Vector3d previousLocation = new Vector3d(
                            container.getDouble(DataQuery.of("previousx")).orElse(0d),
                            container.getDouble(DataQuery.of("previousy")).orElse(-1d),
                            container.getDouble(DataQuery.of("previousz")).orElse(0d)
                    );
                    container.remove(DataQuery.of("world"));
                    container.remove(DataQuery.of("previousx"));
                    container.remove(DataQuery.of("previousy"));
                    container.remove(DataQuery.of("previousz"));
                    container.set(NucleusJailing.PREVIOUS_LOCATION_WORLD, previousWorld);
                    container.set(NucleusJailing.PREVIOUS_LOCATION_POSITION, previousLocation);
                }
            }

            final ServerLocation previous =
                    container.getObject(NucleusJailing.PREVIOUS_LOCATION_WORLD, ResourceKey.class)
                            .filter(x -> Sponge.server().worldManager().worldExists(x))
                            .map(x ->
                                ServerLocation.of(x, container.getObject(NucleusJailing.PREVIOUS_LOCATION_POSITION, Vector3d.class).orElse(DataBuilder.DEFAULT))
                            )
                            .orElse(null);

            return Optional.of(new NucleusJailing(
                    container.getString(NucleusJailing.REASON).get(),
                    container.getString(NucleusJailing.JAIL_NAME).get(),
                    container.getObject(NucleusJailing.JAILER, UUID.class).orElse(null),
                    previous,
                    container.getLong(NucleusJailing.CREATION_INSTANT).map(Instant::ofEpochSecond).orElse(null),
                    container.getObject(NucleusTimedEntry.TIMED_ENTRY, TimedEntry.class).orElse(null),
                    this.shouldSaveStopped)
            );
        }
    }


}
