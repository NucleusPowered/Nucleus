/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.storage;

import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IGeneralDataObject;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IWorldDataObject;
import io.github.nucleuspowered.nucleus.storage.queryobjects.IUserQueryObject;
import io.github.nucleuspowered.nucleus.storage.queryobjects.IWorldQueryObject;
import io.github.nucleuspowered.nucleus.storage.services.persistent.IGeneralDataService;
import io.github.nucleuspowered.storage.dataaccess.IDataAccess;
import io.github.nucleuspowered.storage.persistence.IStorageRepository;
import io.github.nucleuspowered.storage.services.storage.IStorageService;

import java.util.UUID;

import javax.annotation.Nullable;

public interface INucleusStorageManager {

    IGeneralDataService getGeneralService();

    IStorageService.Keyed<UUID, IUserQueryObject, IUserDataObject> getUserService();

    IStorageService.Keyed<UUID, IWorldQueryObject, IWorldDataObject> getWorldService();

    IDataAccess<IUserDataObject> getUserDataAccess();

    IDataAccess<IWorldDataObject> getWorldDataAccess();

    IDataAccess<IGeneralDataObject> getGeneralDataAccess();

    @Nullable IStorageRepository.Keyed<UUID, IUserQueryObject> getUserRepository();

    @Nullable IStorageRepository.Keyed<UUID, IWorldQueryObject> getWorldRepository();

    @Nullable IStorageRepository.Single getGeneralRepository();

}
