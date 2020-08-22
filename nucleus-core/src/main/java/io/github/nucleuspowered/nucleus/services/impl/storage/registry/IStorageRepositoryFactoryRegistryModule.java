/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.storage.registry;

import io.github.nucleuspowered.nucleus.scaffold.registry.NucleusRegistryModule;
import io.github.nucleuspowered.nucleus.services.impl.storage.persistence.FlatFileStorageRepositoryFactory;
import io.github.nucleuspowered.storage.persistence.IStorageRepositoryFactory;

import com.google.inject.Singleton;

@Singleton
public class IStorageRepositoryFactoryRegistryModule extends NucleusRegistryModule<IStorageRepositoryFactory> {

    private static IStorageRepositoryFactoryRegistryModule INSTANCE;
    private final FlatFileStorageRepositoryFactory defaultFactory;

    public static IStorageRepositoryFactoryRegistryModule getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("Instance is not yet initialised");
        }
        return INSTANCE;
    }

    public IStorageRepositoryFactoryRegistryModule(FlatFileStorageRepositoryFactory factory) {
        if (INSTANCE != null) {
            throw new IllegalStateException("Singleton already exists");
        }
        this.defaultFactory = factory;
        INSTANCE = this;
    }

    @Override
    public Class<IStorageRepositoryFactory> catalogClass() {
        return IStorageRepositoryFactory.class;
    }

    @Override
    public void registerModuleDefaults() {
        this.registerAdditionalCatalog(this.defaultFactory);
    }
}
