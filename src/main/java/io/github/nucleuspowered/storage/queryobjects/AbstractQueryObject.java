/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.queryobjects;

import com.google.common.collect.ImmutableMap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractQueryObject<K, T extends IQueryObject<K, T>> implements IQueryObject<K, T> {

    private final Set<K> keys = new HashSet<>();
    private final Map<QueryKey<?, T>, List<?>> queryKeyObjectMap = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <R> void addConstraint(QueryKey<R, T> key, R value) {
        this.queryKeyObjectMap.compute(key, (k, oldValue) -> {
            if (oldValue == null) {
                oldValue = k.createList();
            }

            ((List<R>) oldValue).add(value);
            return oldValue;
        });
    }

    @Override
    public Map<QueryKey<?, T>, List<?>> queries() {
        return ImmutableMap.copyOf(this.queryKeyObjectMap);
    }

    @Override public void addKey(K uuid) {
        this.keys.add(uuid);
    }

    @Override public void addAllKeys(Collection<K> collection) {
        this.keys.addAll(collection);
    }

    @Override
    public final Collection<K> keys() {
        return Collections.unmodifiableCollection(this.keys);
    }
}
