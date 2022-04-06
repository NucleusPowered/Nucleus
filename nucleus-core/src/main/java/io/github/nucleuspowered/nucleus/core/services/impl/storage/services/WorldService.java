/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.services;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IWorldDataObject;
import io.github.nucleuspowered.storage.query.IWorldQueryObject;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IDataVersioning;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IStorageManager;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.plugin.PluginContainer;

import java.util.function.BiConsumer;

public class WorldService extends AbstractKeyedService<ResourceKey, IWorldQueryObject, IWorldDataObject, DataContainer> {

    public WorldService(final IStorageManager repository, final PluginContainer pluginContainer, final IDataVersioning dataVersioning) {
        super(repository::getWorldDataAccess, repository::getWorldRepository, dataVersioning::migrate, dataVersioning::setVersion, pluginContainer);
    }

    @Override
    protected void onEviction(final ResourceKey key, final IWorldDataObject dataObject, final BiConsumer<ResourceKey, IWorldDataObject> reAdd) {
        Sponge.server().worldManager().world(key).ifPresent(x -> reAdd.accept(key, dataObject));
    }

}
