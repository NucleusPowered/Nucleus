/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataaccess.IDataTranslator;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IDataObject;
import io.github.nucleuspowered.storage.persistence.IStorageRepository;
import io.github.nucleuspowered.storage.persistence.IStorageRepositoryFactory;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.services.IStorageService;
import org.spongepowered.api.data.persistence.DataContainer;

public interface IStorageModule<T extends IDataObject, S extends IStorageService<T>, R extends IStorageRepository,
        D extends IDataTranslator<T, DataContainer>> {

    S getService();

    D getDataTranslator();

    R getRepository();

    void setRepository(IStorageRepositoryFactory factory);

    void detach();
}
