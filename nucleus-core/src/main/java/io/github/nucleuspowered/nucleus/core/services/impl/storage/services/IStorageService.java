/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.services;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IDataObject;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.DataKey;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.IKeyedDataObject;
import io.github.nucleuspowered.storage.query.IQueryObject;
import io.github.nucleuspowered.storage.util.KeyedObject;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * The entry point into the storage system. All storage checks are not dependent on the main thread,
 * that is, they should never use game objects.
 */
public interface IStorageService<D extends IDataObject> {

    /**
     * Creates a new {@link D} object.
     *
     * @return A new {@link D}
     */
    D createNew();

    /**
     * Provides a hint to the storage engine that anything that has been not yet been
     * saved must now be saved.
     */
    CompletableFuture<Void> ensureSaved();

    /**
     * Provides a hint to the storage engine that anything that has been cached
     * should be cleared. Implementors may not do anything.
     */
    CompletableFuture<Void> clearCache();

    /**
     * A service where there is one data point.
     *
     * @param <D> The data object type
     */
    interface Single<D extends IDataObject> extends IStorageService<D> {

        /**
         * Gets the data.
         *
         * @return A {@link CompletableFuture} that contains the data, if it exists.
         */
        CompletableFuture<Optional<D>> get();

        /**
         * Gets the data on thread.
         *
         * @return The data, if it exists.
         */
        Optional<D> getOnThread();

        /**
         * Gets the data, or a new {@link D}.
         *
         * @return The {@link D}
         */
        default CompletableFuture<D> getOrNew() {
            return this.get().thenApply(d -> d.orElseGet(() -> {
                final D result = this.createNew();
                this.save(result);
                return result;
            }));
        }

        /**
         * Gets the data on thread, or creates a new {@link D}.
         *
         * @return The data.
         */
        default D getOrNewOnThread() {
            return this.getOnThread().orElseGet(() -> {
                final D result = this.createNew();
                this.save(result);
                return result;
            });
        }

        /**
         * Save the data.
         *
         * @param value The data to save.
         * @return A {@link CompletableFuture} that may contain an exception.
         */
        CompletableFuture<Void> save(@NonNull D value);
    }

    /**
     * A service where there is one data point that can be cached.
     *
     * @param <D> The data object type
     */
    interface SingleCached<D extends IDataObject> extends IStorageService.Single<D> {

        /**
         * Reloads the data from persistence, only clearing the
         * cache if successful.
         */
        CompletableFuture<Void> reload();

        /**
         * Saves the cached version if it exists.
         */
        void saveCached();

        /**
         * Get the cached object, if it exists.
         *
         * @return The object.
         */
        Optional<D> getCached();
    }

    /**
     * A service where multiple data objects may be retured based on a query.
     *
     * <p>Note that while a query object may be provided, the storage engine
     * backing the service <em>may not</em> support them. An exception will be
     * returned in this scenario.</p>
     *
     * <p>Users should check {@link #supportsNonPrimaryKeyQueries()} before using
     * query objects.</p>
     *
     * @param <K> The primary key type
     * @param <Q> The {@link IQueryObject} that can contain query parameters
     * @param <D> The {@link IDataObject} that this service deals with.
     */
    interface Keyed<K, Q extends IQueryObject<K, Q>, D extends IDataObject> extends IStorageService<D> {

        /**
         * Whether the backing storage engine supports queries that is not simply
         * the primary key.
         *
         * @return true if non-primary key queries are supported.
         */
        default boolean supportsNonPrimaryKeyQueries() {
            return false;
        }

        /**
         * Gets the object based on the provided key, if it exists.
         *
         * @param key The key
         * @return The object, if it exists
         */
        CompletableFuture<Optional<D>> get(@NonNull K key);

        /**
         * Gets the object based on the provided key, if it exists.
         *
         * @param key The key
         * @return The object, if it exists
         */
        Optional<D> getOnThread(@NonNull K key);

        /**
         * Gets the data, or a new {@link D}.
         *
         * @param key The key
         * @return The {@link D}
         */
        default CompletableFuture<D> getOrNew(@NonNull final K key) {
            return this.get(key).thenApply(d -> d.orElseGet(() -> {
                final D result = this.createNew();
                this.save(key, result);
                return result;
            }));
        }

        /**
         * Gets the object based on the provided key, if it exists.
         *
         * @param key The key
         * @return The object, if it exists
         */
        default D getOrNewOnThread(@NonNull final K key) {
            return this.getOnThread(key).orElseGet(() -> {
                final D result = this.createNew();
                this.save(key, result);
                return result;
            });
        }

        /**
         * Gets an object based on the supplied query, if one can be uniquely identified.
         *
         * <p>If {@link #supportsNonPrimaryKeyQueries()} and {@link Q#restrictedToKeys()} are both false,
         * the future will contain an error.</p>
         *
         * @param query The query
         * @return The {@link CompletableFuture} containing the result, or an exception.
         */
        CompletableFuture<Optional<KeyedObject<K, D>>> get(@NonNull Q query);

        /**
         * Gets all objects that match the specified query.
         *
         * <p>If {@link #supportsNonPrimaryKeyQueries()} and {@link Q#restrictedToKeys()} are both false,
         * the future will contain an error.</p>
         *
         * @param query The query
         * @return The {@link CompletableFuture} containing the results, if any
         */
        CompletableFuture<Map<K, D>> getAll(@NonNull Q query);

        /**
         * Gets whether the object with the associated key exists.
         *
         * @param key The key
         * @return The {@link CompletableFuture} containing the whether the object exists
         */
        CompletableFuture<Boolean> exists(@NonNull K key);

        /**
         * Gets whether at least one result can be returned by the associated query.
         *
         * <p>If {@link #supportsNonPrimaryKeyQueries()} and {@link Q#restrictedToKeys()} are both false,
         * the future will contain an error.</p>
         *
         * @param query The query
         * @return The {@link CompletableFuture} containing the whether an object exists
         */
        default CompletableFuture<Boolean> exists(@NonNull final Q query) {
            return this.count(query).thenApply(x -> x > 0);
        }

        /**
         * Gets the number of results can be returned by the associated query.
         *
         * <p>If {@link #supportsNonPrimaryKeyQueries()} and {@link Q#restrictedToKeys()} are both false,
         * the future will contain an error.</p>
         *
         * @param query The query
         * @return The {@link CompletableFuture} containing the count
         */
        CompletableFuture<Integer> count(@NonNull Q query);

        /**
         * Saves an object of type {@link D} against the primary key of type {@link K}.
         *
         * @param key The key to save the object under
         * @param value The value of the object
         * @return A {@link CompletableFuture} that will contain an exception if there was a failure
         */
        CompletableFuture<Void> save(@NonNull K key, @NonNull D value);

        /**
         * Deletes the object associated with the key {@link K}.
         *
         * @param key The key to delete the object for
         * @return A {@link CompletableFuture} that will contain an exception if there was a failure
         */
        CompletableFuture<Void> delete(@NonNull K key);

        /**
         * Clears the cache for objects that do not have keys that
         * match those in the provided set.
         *
         * @param keysToKeep The keys
         */
        CompletableFuture<Void> clearCacheUnless(Set<K> keysToKeep);

        /**
         * Indicates the data is also keyed.
         *
         * @param <K> The primary key type
         * @param <Q> The {@link IQueryObject} that can contain query parameters
         * @param <D> The {@link IDataObject} that this service deals with.
         */
        interface KeyedData<K, Q extends IQueryObject<K, Q>, D extends IKeyedDataObject<D>> extends Keyed<K, Q, D> {

            /**
             * Performs a {@link #getOrNew(Object)}, then a {@link IKeyedDataObject#set(DataKey, Object)}, followed by a
             * {@link #save(Object, IDataObject)}
             *
             * @param key The key to save against
             * @param dataKey The data key to save against
             * @param data The data to save
             * @param <T2> The type of data to save
             * @return A {@link CompletableFuture} that will contain an exception if there was a failure
             */
            <T2> CompletableFuture<Void> setAndSave(@NonNull K key, DataKey<T2, ? extends D> dataKey, T2 data);

            <T2> CompletableFuture<Void> removeAndSave(@NonNull K key, DataKey<T2, ? extends D> dataKey);

        }
    }

}
