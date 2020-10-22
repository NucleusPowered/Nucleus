/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.storage.services;

import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.services.impl.storage.queryobjects.IUserQueryObject;
import io.github.nucleuspowered.nucleus.services.interfaces.IDataVersioning;
import io.github.nucleuspowered.nucleus.services.interfaces.IStorageManager;
import io.github.nucleuspowered.storage.services.AbstractKeyedService;
import org.spongepowered.api.plugin.PluginContainer;

public class UserService extends AbstractKeyedService<IUserQueryObject, IUserDataObject> {

    public UserService(IStorageManager repository, PluginContainer pluginContainer, IDataVersioning dataVersioning) {
        super(repository::getUserDataAccess, repository::getUserRepository, dataVersioning::migrate, dataVersioning::setVersion, pluginContainer);
    }
}
