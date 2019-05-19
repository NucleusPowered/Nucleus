/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.storage.services;

import io.github.nucleuspowered.nucleus.storage.INucleusStorageManager;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.storage.queryobjects.IUserQueryObject;
import io.github.nucleuspowered.storage.services.AbstractKeyedService;

public class UserService extends AbstractKeyedService<IUserQueryObject, IUserDataObject> {

    public <O> UserService(INucleusStorageManager<O> repository) {
        super(repository::getUserDataAccess, repository::getUserRepository);
    }
}
