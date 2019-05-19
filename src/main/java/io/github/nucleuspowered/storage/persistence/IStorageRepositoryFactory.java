/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.persistence;

import io.github.nucleuspowered.nucleus.storage.queryobjects.IUserQueryObject;
import io.github.nucleuspowered.nucleus.storage.queryobjects.IWorldQueryObject;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.CatalogType;

import java.util.UUID;

/**
 * Contains methods to create appropriate {@link IStorageRepository}s for user, world and general data objects.
 *
 * <p>Any of these methods may return {@code null} to indicate that the storage type is
 * <strong>not supported</strong> for this specific data type.</p>
 */
public interface IStorageRepositoryFactory<O> extends CatalogType {

    /**
     * Gets a storage system for user data
     *
     * @return The storage system, if offered.
     */
    IStorageRepository.@Nullable Keyed<UUID, IUserQueryObject, O> userRepository();

    /**
     * Gets a storage system for world data
     *
     * @return The storage system, if offered.
     */
    IStorageRepository.@Nullable Keyed<UUID, IWorldQueryObject, O> worldRepository();

    /**
     * Gets a storage system for general data
     *
     * @return The storage system, if offered.
     */
    IStorageRepository.@Nullable Single<O> generalRepository();

}
