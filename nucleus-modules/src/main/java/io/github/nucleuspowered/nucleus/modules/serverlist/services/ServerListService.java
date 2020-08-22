/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.serverlist.services;

import io.github.nucleuspowered.nucleus.modules.serverlist.ServerListKeys;
import io.github.nucleuspowered.nucleus.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IGeneralDataObject;
import io.github.nucleuspowered.nucleus.services.interfaces.IStorageManager;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.time.Instant;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.inject.Inject;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "OptionalAssignedToNull"})
public class ServerListService implements ServiceBase {

    @Nullable private Optional<Text> messageCache = null;
    private Instant expiry = Instant.MAX;
    private final IStorageManager storageManager;

    @Inject
    public ServerListService(INucleusServiceCollection serviceCollection) {
        this.storageManager = serviceCollection.storageManager();
    }


    public void clearMessage() {
        IGeneralDataObject dataObject = this.storageManager.getGeneralService().getOrNewOnThread();
        dataObject.remove(ServerListKeys.EXPIRY);
        dataObject.remove(ServerListKeys.LINE_ONE);
        dataObject.remove(ServerListKeys.LINE_TWO);
        this.expiry = Instant.MAX;
        this.messageCache = null;
    }

    public void updateLineOne(@Nullable String line1) {
        if (checkMessage()) {
            this.storageManager.getGeneralService().getOrNew().thenAccept(x -> x.set(ServerListKeys.LINE_ONE, line1));
        }
    }

    public void updateLineTwo(@Nullable String line2) {
        if (checkMessage()) {
            this.storageManager.getGeneralService().getOrNew().thenAccept(x -> x.set(ServerListKeys.LINE_TWO, line2));
        }
    }

    private boolean checkMessage() {
        if (this.messageCache == null) {
            constructMessage();
        }

        return this.messageCache.isPresent();
    }

    public void setMessage(@Nullable String line1, @Nullable String line2, @Nullable Instant expiry) {
        IGeneralDataObject dataObject = this.storageManager.getGeneralService().getOrNewOnThread();
        dataObject.set(ServerListKeys.EXPIRY, expiry);
        dataObject.set(ServerListKeys.LINE_ONE, line1);
        dataObject.set(ServerListKeys.LINE_TWO, line2);
        this.expiry = expiry == null ? Instant.MAX : expiry;
        this.messageCache = null;
    }

    private void constructMessage() {
        IGeneralDataObject dataObject = this.storageManager.getGeneralService().getOrNewOnThread();
        this.expiry = dataObject.get(ServerListKeys.EXPIRY).orElse(Instant.MAX);
        constructMessage(
                dataObject.get(ServerListKeys.LINE_ONE).map(TextSerializers.FORMATTING_CODE::deserialize).orElse(null),
                dataObject.get(ServerListKeys.LINE_TWO).map(TextSerializers.FORMATTING_CODE::deserialize).orElse(null));
    }

    private void constructMessage(Text lineOne, Text lineTwo) {
        if (lineOne != null || lineTwo != null) {
            this.messageCache =
                    Optional.of(Text.of(lineOne == null ? Text.EMPTY : lineOne,
                            Text.NEW_LINE,
                            lineTwo == null ? Text.EMPTY : lineTwo));
        } else {
            this.messageCache = Optional.empty();
        }
    }

    public Optional<Text> getMessage() {
        if (this.expiry.isBefore(Instant.now())) {
            clearMessage();
        }

        if (this.messageCache != null) {
            constructMessage();
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
