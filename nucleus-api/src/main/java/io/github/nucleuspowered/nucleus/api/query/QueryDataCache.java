package io.github.nucleuspowered.nucleus.api.query;

/**
 * A data cache which stores objects constructed from data retrieved from a query.
 *
 * @param <K> The type of the key used to retrieve cached objects.
 * @param <V> The type of the object being cached.
 */
public interface QueryDataCache<K, V> {

    /**
     * Attempts to get an item from the cache with the provided key, if it isn't present it is then loaded into the
     * cache from the database.
     *
     * @param key The key for the items location in cache, this is a unique identifier.
     * @return The retrieved object.
     */
    V getOrLoad(K key);

    /**
     * Updates the data for an object stored in cache based on the current data in the database.
     *
     * @param key The key for the items location in cache, this is a unique identifier.
     * @return The newly refreshed object.
     */
    V refresh(K key);

    /**
     * Syncs all changes made to objects in the cache to the database.
     */
    void sync();
}
