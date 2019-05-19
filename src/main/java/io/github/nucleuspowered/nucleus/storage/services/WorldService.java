/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.storage.services;

import io.github.nucleuspowered.nucleus.storage.INucleusStorageManager;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IWorldDataObject;
import io.github.nucleuspowered.nucleus.storage.queryobjects.IWorldQueryObject;
import io.github.nucleuspowered.storage.services.AbstractKeyedService;

public class WorldService extends AbstractKeyedService<IWorldQueryObject, IWorldDataObject> {

    public <O> WorldService(INucleusStorageManager<O> repository) {
        super(repository::getWorldDataAccess, repository::getWorldRepository);
    }

}
