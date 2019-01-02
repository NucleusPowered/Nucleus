/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.storage.registry;

import io.github.nucleuspowered.nucleus.internal.registry.NucleusRegistryModule;
import io.github.nucleuspowered.storage.persistence.IStorageRepositoryFactory;
import io.github.nucleuspowered.storage.persistence.configurate.FlatFileStorageRepositoryFactory;

import javax.inject.Singleton;

@Singleton
public class IStorageRepositoryFactoryRegistryModule extends NucleusRegistryModule<IStorageRepositoryFactory> {

    private static IStorageRepositoryFactoryRegistryModule INSTANCE;

    public static IStorageRepositoryFactoryRegistryModule getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("Instance is not yet initialised");
        }
        return INSTANCE;
    }

    public IStorageRepositoryFactoryRegistryModule() {
        if (INSTANCE != null) {
            throw new IllegalStateException("Singleton already exists");
        }
        INSTANCE = this;
    }

    @Override
    public Class<IStorageRepositoryFactory> catalogClass() {
        return IStorageRepositoryFactory.class;
    }

    @Override
    public void registerDefaults() {
        this.registerAdditionalCatalog(FlatFileStorageRepositoryFactory.INSTANCE);
    }
}
