/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.storage.persistence;

import com.google.gson.JsonObject;
import io.github.nucleuspowered.nucleus.guice.DataDirectory;
import io.github.nucleuspowered.nucleus.services.impl.storage.queryobjects.IUserQueryObject;
import io.github.nucleuspowered.nucleus.services.impl.storage.queryobjects.IWorldQueryObject;
import io.github.nucleuspowered.storage.exceptions.DataQueryException;
import io.github.nucleuspowered.storage.persistence.IStorageRepository;
import io.github.nucleuspowered.storage.persistence.IStorageRepositoryFactory;
import io.github.nucleuspowered.storage.queryobjects.IQueryObject;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public final class FlatFileStorageRepositoryFactory implements IStorageRepositoryFactory<JsonObject> {

    private static final String WORLD_DATA_DIRECTORY = "worlddata";
    private static final String USER_DATA_DIRECTORY = "userdata";
    private static final String GENERAL_FILE = "general.json";
    private static final String KITS_FILE = "kits.json";
    private final Supplier<Path> dataPath;
    private final Logger logger;

    @Inject
    public FlatFileStorageRepositoryFactory(@DataDirectory Supplier<Path> path, Logger logger) {
        this.dataPath = path;
        this.logger = logger;
    }

    @Override
    public IStorageRepository.Keyed<UUID, IUserQueryObject, JsonObject> userRepository() {
        return repository(USER_DATA_DIRECTORY);
    }

    @Override
    public IStorageRepository.Keyed<UUID, IWorldQueryObject, JsonObject> worldRepository() {
        return repository(WORLD_DATA_DIRECTORY);
    }

    private <R extends IQueryObject<UUID, R>> IStorageRepository.Keyed<UUID, R, JsonObject> repository(final String p) {
        return new FlatFileStorageRepository.UUIDKeyed<>(this.logger, query -> {
            if (query.keys().size() == 1) {
                Collection<UUID> uuids = query.keys();
                String uuid = uuids.iterator().next().toString();
                return this.dataPath.get().resolve(p).resolve(uuid.substring(0, 2)).resolve(uuid  + ".json");
            }

            throw new DataQueryException("There must only a key", query);
        },
        uuid -> this.dataPath.get().resolve(p).resolve(uuid.toString().substring(0, 2)).resolve(uuid.toString() + ".json"),
        () -> this.dataPath.get().resolve(p));
    }

    @Override
    public IStorageRepository.Single<JsonObject> generalRepository() {
        return new FlatFileStorageRepository.Single(this.logger, () -> this.dataPath.get().resolve(GENERAL_FILE));
    }

    @Override
    public IStorageRepository.Single<JsonObject> kitsRepository() {
        return new FlatFileStorageRepository.Single(this.logger, () -> this.dataPath.get().resolve(KITS_FILE));
    }

    @Override public String getId() {
        return "nucleus:flatfile";
    }

    @Override public String getName() {
        return "Flat File";
    }
}
