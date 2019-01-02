/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.queryobjects;

import io.github.nucleuspowered.storage.persistence.IStorageRepository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IQueryObject<K, T extends IQueryObject<K, T>> {

    /**
     * Any queries that aren't the primary key.
     *
     * <p>Ensure {@link IStorageRepository.Keyed#supportsNonKeyQueries()} is true if using this</p>
     *
     * @return
     */
    Map<QueryKey<?, T>, List<?>> queries();

    /**
     * Adds a constraint to the query
     *
     * @param key The key to add the constraint to
     * @param value The value of the constraint
     * @param <R> The type of the constraint
     */
    <R> void addConstraint(QueryKey<R, T> key, R value);

    /**
     * The type of primary key
     *
     * @return The class of the key type
     */
    Class<K> keyType();

    /**
     * Add a primary key to this query.
     *
     * @param key The key to add
     */
    void addKey(K key);

    /**
     * Adds multiple primary keys to this query.
     *
     * @param collection The keys to add.
     */
    void addAllKeys(Collection<K> collection);

    /**
     * Keys to restrict to
     *
     * @return The keys to restrict to
     */
    Collection<K> keys();

    /**
     * Whether the query is dependent on keys.
     *
     * @return The keys.
     */
    default boolean restrictedToKeys() {
        return !keys().isEmpty();
    }

}

