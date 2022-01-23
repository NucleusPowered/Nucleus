/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.serverlist.services;

import io.github.nucleuspowered.nucleus.modules.serverlist.ServerListKeys;
import io.github.nucleuspowered.nucleus.core.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IGeneralDataObject;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IStorageManager;
import net.kyori.adventure.text.Component;

import java.time.Instant;
import java.util.Optional;

import com.google.inject.Inject;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "OptionalAssignedToNull"})
public class ServerListService implements ServiceBase {

    @Nullable private Optional<Component> messageCache = null;
    private Instant expiry = Instant.MAX;
    private final IStorageManager storageManager;

    @Inject
    public ServerListService(final INucleusServiceCollection serviceCollection) {
        this.storageManager = serviceCollection.storageManager();
    }


    public void clearMessage() {
        final IGeneralDataObject dataObject = this.storageManager.getGeneralService().getOrNewOnThread();
        dataObject.remove(ServerListKeys.EXPIRY);
        dataObject.remove(ServerListKeys.LINE_ONE);
        dataObject.remove(ServerListKeys.LINE_TWO);
        this.expiry = Instant.MAX;
        this.messageCache = null;
    }

    public void updateLineOne(@Nullable final String line1) {
        if (this.checkMessage()) {
            this.storageManager.getGeneralService().getOrNew().thenAccept(x -> x.set(ServerListKeys.LINE_ONE, line1));
        }
    }

    public void updateLineTwo(@Nullable final String line2) {
        if (this.checkMessage()) {
            this.storageManager.getGeneralService().getOrNew().thenAccept(x -> x.set(ServerListKeys.LINE_TWO, line2));
        }
    }

    private boolean checkMessage() {
        if (this.messageCache == null) {
            this.constructMessage();
        }

        return this.messageCache.isPresent();
    }

    public void setMessage(@Nullable final String line1, @Nullable final String line2, @Nullable final Instant expiry) {
        final IGeneralDataObject dataObject = this.storageManager.getGeneralService().getOrNewOnThread();
        dataObject.set(ServerListKeys.EXPIRY, expiry);
        dataObject.set(ServerListKeys.LINE_ONE, line1);
        dataObject.set(ServerListKeys.LINE_TWO, line2);
        this.expiry = expiry == null ? Instant.MAX : expiry;
        this.messageCache = null;
    }

    private void constructMessage() {
        final IGeneralDataObject dataObject = this.storageManager.getGeneralService().getOrNewOnThread();
        this.expiry = dataObject.get(ServerListKeys.EXPIRY).orElse(Instant.MAX);
        this.constructMessage(
                dataObject.get(ServerListKeys.LINE_ONE).map(LegacyComponentSerializer.legacyAmpersand()::deserialize).orElse(null),
                dataObject.get(ServerListKeys.LINE_TWO).map(LegacyComponentSerializer.legacyAmpersand()::deserialize).orElse(null));
    }

    private void constructMessage(final Component lineOne, final Component lineTwo) {
        if (lineOne != null || lineTwo != null) {
            this.messageCache =
                    Optional.of(LinearComponents.linear(lineOne == null ? Component.empty() : lineOne,
                            Component.newline(),
                            lineTwo == null ? Component.empty() : lineTwo));
        } else {
            this.messageCache = Optional.empty();
        }
    }

    public Optional<Component> getMessage() {
        if (this.expiry.isBefore(Instant.now())) {
            this.clearMessage();
        }

        if (this.messageCache == null) {
            this.constructMessage();
        }

        return this.messageCache;
    }

    public Optional<Instant> getExpiry() {
        if (this.expiry == Instant.MAX) {
            return Optional.empty();
        }
        return Optional.of(this.expiry);
    }
}
