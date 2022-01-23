/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.services;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IUserDataObject;
import io.github.nucleuspowered.storage.query.IUserQueryObject;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IDataVersioning;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IStorageManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.plugin.PluginContainer;

import java.util.UUID;
import java.util.function.BiConsumer;

public final class UserService extends AbstractKeyedService<UUID, IUserQueryObject, IUserDataObject, DataContainer> {

    public UserService(final IStorageManager repository, final PluginContainer pluginContainer, final IDataVersioning dataVersioning) {
        super(repository::getUserDataAccess, repository::getUserRepository, dataVersioning::migrate, dataVersioning::setVersion, pluginContainer);
    }

    @Override
    protected void onEviction(final UUID key, final IUserDataObject dataObject, final BiConsumer<UUID, IUserDataObject> reAdd) {
        Sponge.server().player(key).ifPresent(x -> reAdd.accept(key, dataObject));
    }

}
