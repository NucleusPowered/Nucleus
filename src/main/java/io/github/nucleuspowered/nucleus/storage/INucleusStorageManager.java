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
import io.github.nucleuspowered.nucleus.storage.services.IGeneralDataService;
import io.github.nucleuspowered.storage.dataaccess.IDataTranslator;
import io.github.nucleuspowered.storage.persistence.IStorageRepository;
import io.github.nucleuspowered.storage.services.IStorageService;

import java.util.UUID;

public interface INucleusStorageManager<O> {

    IGeneralDataService getGeneralService();

    IStorageService.Keyed<UUID, IUserQueryObject, IUserDataObject> getUserService();

    IStorageService.Keyed<UUID, IWorldQueryObject, IWorldDataObject> getWorldService();

    IDataTranslator<IUserDataObject, O> getUserDataAccess();

    IDataTranslator<IWorldDataObject, O> getWorldDataAccess();

    IDataTranslator<IGeneralDataObject, O> getGeneralDataAccess();

    IStorageRepository.Keyed<UUID, IUserQueryObject, O> getUserRepository();

    IStorageRepository.Keyed<UUID, IWorldQueryObject, O> getWorldRepository();

    IStorageRepository.Single<O> getGeneralRepository();

}
