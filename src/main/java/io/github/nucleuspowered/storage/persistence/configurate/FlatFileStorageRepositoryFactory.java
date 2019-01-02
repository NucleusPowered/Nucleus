/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.persistence.configurate;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.storage.queryobjects.IUserQueryObject;
import io.github.nucleuspowered.nucleus.storage.queryobjects.IWorldQueryObject;
import io.github.nucleuspowered.storage.exceptions.DataQueryException;
import io.github.nucleuspowered.storage.persistence.IStorageRepository;
import io.github.nucleuspowered.storage.persistence.IStorageRepositoryFactory;
import io.github.nucleuspowered.storage.queryobjects.IQueryObject;

import java.util.Collection;
import java.util.UUID;

public final class FlatFileStorageRepositoryFactory implements IStorageRepositoryFactory {

    public static final IStorageRepositoryFactory INSTANCE = new FlatFileStorageRepositoryFactory();

    private static final String wd = "worlddata";
    private static final String ud = "userdata";

    private FlatFileStorageRepositoryFactory() {}

    @Override
    public IStorageRepository.Keyed<UUID, IUserQueryObject> userRepository() {
        return repository(ud);
    }

    @Override
    public IStorageRepository.Keyed<UUID, IWorldQueryObject> worldRepository() {
        return repository(wd);
    }

    private <R extends IQueryObject<UUID, R>> IStorageRepository.Keyed<UUID, R> repository(final String p) {
        return new FlatFileStorageRepository.UUIDKeyed<>(query -> {
            if (query.keys().size() == 1) {
                Collection<UUID> uuids = query.keys();
                String uuid = uuids.iterator().next().toString();
                return Nucleus.getNucleus().getDataPath().resolve(p).resolve(uuid.substring(0, 2)).resolve(uuid  + ".json");
            }

            throw new DataQueryException("There must only a key", query);
        },
        uuid -> Nucleus.getNucleus().getDataPath().resolve(p).resolve(uuid.toString().substring(0, 2)).resolve(uuid.toString() + ".json"),
        () -> Nucleus.getNucleus().getDataPath().resolve(p));
    }

    @Override
    public IStorageRepository.Single generalRepository() {
        return new FlatFileStorageRepository.Single(() -> Nucleus.getNucleus().getDataPath().resolve("general.json"));
    }

    @Override public String getId() {
        return "nucleus:flatfile";
    }

    @Override public String getName() {
        return "Flat File";
    }
}
