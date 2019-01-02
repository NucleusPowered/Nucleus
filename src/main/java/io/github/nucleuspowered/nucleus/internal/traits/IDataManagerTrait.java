/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.traits;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IGeneralDataObject;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IWorldDataObject;
import io.github.nucleuspowered.nucleus.storage.services.persistent.IGeneralDataService;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IDataManagerTrait {

    default CompletableFuture<IUserDataObject> getOrCreateUser(UUID uuid) {
        return Nucleus.getNucleus().getStorageManager().getUserService().getOrNew(uuid);
    }

    default IUserDataObject getOrCreateUserOnThread(UUID uuid) {
        return Nucleus.getNucleus().getStorageManager().getUserService().getOrNewOnThread(uuid);
    }

    default CompletableFuture<Optional<IUserDataObject>> getUser(UUID uuid) {
        return Nucleus.getNucleus().getStorageManager().getUserService().get(uuid);
    }

    default Optional<IUserDataObject> getUserOnThread(UUID uuid) {
        return Nucleus.getNucleus().getStorageManager().getUserService().getOnThread(uuid);
    }

    default CompletableFuture<Void> saveUser(UUID uuid, IUserDataObject object) {
        return Nucleus.getNucleus().getStorageManager().getUserService().save(uuid, object);
    }

    default Optional<IWorldDataObject> getWorldOnThread(UUID uuid) {
        return Nucleus.getNucleus().getStorageManager().getWorldService().getOnThread(uuid);
    }

    default IWorldDataObject getOrCreateWorldOnThread(UUID uuid) {
        return Nucleus.getNucleus().getStorageManager().getWorldService().getOrNewOnThread(uuid);
    }

    default IGeneralDataObject getGeneral() {
        IGeneralDataService gs = Nucleus.getNucleus().getStorageManager().getGeneralService();
        return gs.getCached().orElseGet(() -> gs.getOrNew().join());
    }

}
