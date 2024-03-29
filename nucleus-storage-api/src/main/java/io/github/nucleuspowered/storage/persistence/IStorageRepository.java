/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.persistence;

import io.github.nucleuspowered.storage.exceptions.DataDeleteException;
import io.github.nucleuspowered.storage.exceptions.DataLoadException;
import io.github.nucleuspowered.storage.exceptions.DataQueryException;
import io.github.nucleuspowered.storage.exceptions.DataSaveException;
import io.github.nucleuspowered.storage.query.IQueryObject;
import io.github.nucleuspowered.storage.util.KeyedObject;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.configurate.ConfigurateException;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Base interface for interfacing with storage engines. Implementors should implement one of the
 * sub interfaces, {@link Single} or {@link Keyed}, depending on the store.
 *
 * <p>Note that all data will be provided as {@link DataContainer}s. It is expected that the container going
 * into storage is equivalent to that coming out. You may inspect the container as you wish (which you may
 * want to do to support queries more effectively)</p>
 */
public interface IStorageRepository {

    /**
     * Perform any startup for this repository.
     *
     * <p>This will be called when the storage engine is being used for some element of Nucleus data, but
     * before any data is accessed or written. Plugins should not respond to a startup call once it has
     * started up, unless it has then been shutdown and your plugin reuses repositories.</p>
     *
     * <p>This method returns whether startup is successful. If your repository fails to start, return
     * <code>false</code> here. Nucleus will fall back to the default storage in this case.</p>
     *
     * @return whether startup was successful.
     */
    boolean startup();

    /**
     * Shutdown the repository, finishing any operations.
     *
     * <p>This will be called in one of three situations:</p>
     * <ul>
     *     <li>The data source has been changed</li>
     *     <li>The game server has been stopped (on the client only)</li>
     *     <li>The game itself is being shutdown</li>
     * </ul>
     *
     * <p>Implementors should note that calling this method is not guaranteed and should
     * consider data consistency accordingly.</p>
     */
    void shutdown();

    /**
     * Requests that any cache that the repository provides is cleared.
     */
    void clearCache();

    /**
     * If true, {@link #clearCache()} will be called automatically by the system when appropriate.
     *
     * <p>This should be {@code false} if {@link #clearCache()} is a no-op, else true</p>
     *
     * @return true if {@link #clearCache()} performs an action, false otherwise.
     */
    boolean hasCache();

    /**
     * A repository where a single document is stored. In general, this will be a game or
     * server scoped document.
     */
    interface Single<O> extends IStorageRepository {

        /**
         * Gets the object, if it exists
         *
         * @return The object, if it exists
         * @throws DataLoadException if the data could not be loaded
         * @throws DataQueryException if the data could not be queried
         */
        Optional<O> get() throws DataLoadException, DataQueryException;

        /**
         * Saves the supplied {@code object}
         *
         * @param object The object to save
         * @throws DataSaveException if the data could not be saved
         */
        void save(O object) throws DataSaveException;
    }

    /**
     * Interface for repositories that should have unique {@link UUID}s
     *
     * @param <Q> The query object
     */
    interface Keyed<K, Q extends IQueryObject<K, Q>, O> extends IStorageRepository {

        /**
         * Requests that the cache should be purged of any values
         * with the supplied keys.
         *
         * @param keys The keys.
         */
        void clearCache(Iterable<K> keys);

        /**
         * Whether this storage mechanism supports complex queries
         *
         * @return whether the mechanism is supported.
         */
        default boolean supportsNonKeyQueries() {
            return false;
        }

        /**
         * Gets whether an object specified by the {@code query} exists.
         *
         * @param query The query.
         * @return Whether the object exists.
         */
        boolean exists(Q query);

        /**
         * Gets an object based on the {@code query}
         *
         * @param query The query
         * @return The object, if it exists, along with its primary key
         * @throws DataLoadException if the data could not be loaded
         * @throws DataQueryException if the data could not be queried
         */
        Optional<KeyedObject<K, O>> get(Q query) throws DataLoadException, DataQueryException;

        /**
         * Gets the number of objects that satisfies the query.
         *
         * @param query The query
         * @return The number of items that satisfy the query, or -1 if the {@link #supportsNonKeyQueries()} is {@code false} and the query is more than
         *         just a key.
         */
        int count(Q query);

        /**
         * Saves the supplied {@code object} in the position suggested by the supplied {@code query}
         *
         * @param key The key that indicates the location to store the object in
         * @param object The object to save
         * @throws DataSaveException if the data could not be saved
         */
        void save(K key, O object) throws DataSaveException;

        /**
         * Deletes the object at the supplied {@code key}
         *
         * @param key The key
         * @throws DataDeleteException if the data could not be deleted
         */
        void delete(K key) throws DataDeleteException;

        /**
         * Gets whether an object specified by the key exists.
         *
         * @param key The key.
         * @return Whether the object exists.
         */
        boolean exists(K key);

        /**
         * Gets an object based on the key
         *
         * @param key The key
         * @return The object, if it exists
         * @throws DataLoadException if the data could not be loaded
         * @throws DataQueryException if the data could not be queried
         */
        Optional<O> get(K key) throws DataLoadException, DataQueryException;

        /**
         * Gets all the stored keys
         *
         * @return The objects, if any exist, or an empty collection if {@link #supportsNonKeyQueries()} is {@code false} and the query is more than
         *         just a key.
         * @throws DataLoadException if the data could not be loaded
         */
        Collection<K> getAllKeys() throws DataLoadException;

        /**
         * Gets the objects that satisfy the {@code query}
         *
         * @param query The query
         * @return The objects, if any exist, or an empty map if {@link #supportsNonKeyQueries()} is {@code false} and the query is more than
         *         just a key.
         * @throws DataLoadException if the data could not be loaded
         * @throws DataQueryException if the data could not be queried
         */
        Map<K, O> getAll(Q query) throws DataLoadException, DataQueryException;

        /**
         * Gets the objects that satisfy the {@code query}
         *
         * @param query The query
         * @return The objects, if any exist, or an empty collection if {@link #supportsNonKeyQueries()} is {@code false} and the query is more than
         *         just a key.
         * @throws DataLoadException if the data could not be loaded
         * @throws DataQueryException if the data could not be queried
         */
        Collection<K> getAllKeys(Q query) throws DataLoadException, DataQueryException;
    }

}
