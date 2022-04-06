/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.persistence;

import io.github.nucleuspowered.nucleus.core.guice.DataDirectory;
import io.github.nucleuspowered.storage.query.IUserQueryObject;
import io.github.nucleuspowered.storage.query.IWorldQueryObject;
import io.github.nucleuspowered.storage.exceptions.DataQueryException;
import io.github.nucleuspowered.storage.persistence.IStorageRepository;
import io.github.nucleuspowered.storage.persistence.IStorageRepositoryFactory;

import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.persistence.DataContainer;

@Singleton
public final class FlatFileStorageRepositoryFactory implements IStorageRepositoryFactory {

    private static final String WORLD_DATA_DIRECTORY = "worlddata";
    private static final String USER_DATA_DIRECTORY = "userdata";
    private static final String GENERAL_FILE = "general.json";
    private static final String KITS_FILE = "kits.json";
    private final Supplier<Path> dataPath;
    private final Logger logger;

    @Inject
    public FlatFileStorageRepositoryFactory(@DataDirectory final Supplier<Path> path, final Logger logger) {
        this.dataPath = path;
        this.logger = logger;
    }

    @Override
    public IStorageRepository.Keyed<ResourceKey, IWorldQueryObject, DataContainer> worldRepository() {
        return new FlatFileStorageRepository.ResourceKeyed<>(this.logger, query -> {
            if (query.keys().size() == 1) {
                final Collection<ResourceKey> keys = query.keys();
                final ResourceKey key = keys.iterator().next();
                return this.dataPath.get().resolve(WORLD_DATA_DIRECTORY).resolve(key.namespace()).resolve(key.value()  + ".json");
            }

            throw new DataQueryException("There must only a key", query);
        },
        key -> this.dataPath.get().resolve(WORLD_DATA_DIRECTORY).resolve(key.namespace()).resolve(key.value() + ".json"),
        () -> this.dataPath.get().resolve(WORLD_DATA_DIRECTORY));
    }

    @Override
    public IStorageRepository.Keyed<UUID, IUserQueryObject, DataContainer> userRepository() {
        return new FlatFileStorageRepository.UUIDKeyed<>(this.logger, query -> {
            if (query.keys().size() == 1) {
                final Collection<UUID> uuids = query.keys();
                final String uuid = uuids.iterator().next().toString();
                return this.dataPath.get().resolve(USER_DATA_DIRECTORY).resolve(uuid.substring(0, 2)).resolve(uuid  + ".json");
            }

            throw new DataQueryException("There must only a key", query);
        },
        uuid -> this.dataPath.get().resolve(USER_DATA_DIRECTORY).resolve(uuid.toString().substring(0, 2)).resolve(uuid.toString() + ".json"),
        () -> this.dataPath.get().resolve(USER_DATA_DIRECTORY));
    }

    @Override
    public IStorageRepository.Single<DataContainer> generalRepository() {
        return new FlatFileStorageRepository.Single(this.logger, () -> this.dataPath.get().resolve(GENERAL_FILE));
    }

    @Override
    public IStorageRepository.Single<DataContainer> kitsRepository() {
        return new FlatFileStorageRepository.Single(this.logger, () -> this.dataPath.get().resolve(KITS_FILE));
    }

}
