/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.services;

import io.github.nucleuspowered.nucleus.api.module.jail.data.Jailing;
import io.github.nucleuspowered.nucleus.api.util.data.TimedEntry;
import io.github.nucleuspowered.nucleus.core.datatypes.NucleusTimedEntry;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.persistence.Queries;
import org.spongepowered.api.world.server.ServerLocation;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

public class NucleusJailing implements Jailing {

    public static final int CONTENT_VERSION = 1;

    private static final DataQuery REASON = DataQuery.of("reason");
    private static final DataQuery JAIL_NAME = DataQuery.of("jailName");
    private static final DataQuery JAILER = DataQuery.of("jailer");
    private static final DataQuery PREVIOUS_LOCATION = DataQuery.of("previousLocation");
    private static final DataQuery CREATION_INSTANT = DataQuery.of("creationInstant");
    private static final DataQuery TIMED_ENTRY = DataQuery.of("timedEntry");

    public static NucleusJailing fromJailingRequest(final UUID user, final String reason, final String jailName,
                                                    @Nullable final UUID jailer, @Nullable final ServerLocation previousLocation,
                                                    @Nullable final Instant creationInstant, @Nullable final TimedEntry timedEntry) {
        return new NucleusJailing(reason, jailName, jailer, previousLocation, creationInstant, timedEntry);
    }


    final String reason;
    final String jailName;
    @Nullable final UUID jailer;
    @Nullable final ServerLocation previousLocation;
    @Nullable final Instant creationInstant;
    @Nullable final TimedEntry expiryTime;

    public NucleusJailing(final String reason, final String jailName, @Nullable final UUID jailer,
                             @Nullable final ServerLocation previousLocation, @Nullable final Instant creationInstant,
                             @Nullable final TimedEntry timedEntry) {
        this.reason = reason;
        this.jailName = jailName;
        this.jailer = jailer;
        this.previousLocation = previousLocation;
        this.creationInstant = creationInstant;
        this.expiryTime = timedEntry;
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

    public DataContainer toContainer() {
        final DataContainer container = DataContainer.createNew()
                .set(Queries.CONTENT_VERSION, NucleusJailing.CONTENT_VERSION)
                .set(NucleusJailing.REASON, this.reason)
                .set(NucleusJailing.JAIL_NAME, this.jailName)
                .set(NucleusJailing.JAILER, this.jailer);
        this.getPreviousLocation().ifPresent(x -> container.set(NucleusJailing.PREVIOUS_LOCATION, x));
        this.getCreationInstant().ifPresent(x -> container.set(NucleusJailing.CREATION_INSTANT, x));
        this.getTimedEntry().ifPresent(x -> container.set(NucleusJailing.TIMED_ENTRY, x));
        return container;
    }

    public static final class DataBuilder extends AbstractDataBuilder<Jailing> implements IReloadableService.Reloadable {

        public DataBuilder() {
            super(Jailing.class, NucleusJailing.CONTENT_VERSION);
        }

        @Override
        protected Optional<Jailing> buildContent(final DataView container) throws InvalidDataException {
            if (!container.contains(Queries.CONTENT_VERSION)) {
                NucleusTimedEntry.upgradeLegacy(container, NucleusJailing.TIMED_ENTRY);
            }

            return Optional.of();
        }

        @Override
        public void onReload(final INucleusServiceCollection serviceCollection) {

        }
    }


}
