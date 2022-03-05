/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mail.data;

import io.github.nucleuspowered.nucleus.api.module.mail.data.MailMessage;
import io.github.nucleuspowered.nucleus.modules.message.MessageKeys;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.persistence.Queries;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class NucleusMailMessage implements MailMessage {

    private static final int CONTENT_VERSION = 1;

    private static final DataQuery SENDER = DataQuery.of("sender");
    private static final DataQuery DATE = DataQuery.of("date");
    private static final DataQuery MESSAGE = DataQuery.of("message");

    private final @Nullable UUID sender;
    private final Instant date;
    private final String message;

    public NucleusMailMessage(final @Nullable UUID sender, final Instant date, final String message) {
        this.sender = sender;
        this.date = date;
        this.message = message;
    }

    @Override public String getMessage() {
        return this.message;
    }

    @Override public Instant getDate() {
        return this.date;
    }

    public UUID getSenderUUID() {
        return this.sender;
    }

    @Override public Optional<UUID> getSender() {
        return Optional.ofNullable(this.sender);
    }

    @Override public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        final NucleusMailMessage nucleusMailMessage = (NucleusMailMessage) o;

        if (this.date != nucleusMailMessage.date) {
            return false;
        }
        if (this.sender == null) {
            return nucleusMailMessage.sender == null;
        }
        return this.sender.equals(nucleusMailMessage.sender) && this.message.equals(nucleusMailMessage.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sender, date, message);
    }

    @Override
    public int contentVersion() {
        return NucleusMailMessage.CONTENT_VERSION;
    }

    @Override
    public DataContainer toContainer() {
        final DataContainer dataContainer = DataContainer.createNew();
        if (this.sender != null) {
            dataContainer.set(NucleusMailMessage.SENDER, this.sender);
        }
        return dataContainer.set(NucleusMailMessage.MESSAGE, this.message)
                .set(NucleusMailMessage.DATE, this.date.toEpochMilli());
    }

    public static final class DataBuilder extends AbstractDataBuilder<MailMessage> {

        public DataBuilder() {
            super(MailMessage.class, NucleusMailMessage.CONTENT_VERSION);
        }

        @Override
        protected Optional<MailMessage> buildContent(final DataView container) throws InvalidDataException {
            if (!container.contains(Queries.CONTENT_VERSION)) {
                container.getObject(DataQuery.of("uuid"), UUID.class).ifPresent(x -> container.set(NucleusMailMessage.SENDER, x));
            }
            return Optional.of(
                    new NucleusMailMessage(
                            container.getObject(NucleusMailMessage.SENDER, UUID.class).orElse(null),
                            container.getLong(NucleusMailMessage.DATE).map(Instant::ofEpochMilli).get(),
                            container.getString(NucleusMailMessage.MESSAGE).get()
                    )
            );
        }
    }

}
