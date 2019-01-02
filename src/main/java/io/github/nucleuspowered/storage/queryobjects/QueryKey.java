/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.queryobjects;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A key used for setting up queries.
 *
 * @param <T> The type of object the key is associated with
 * @param <Q> The {@link IQueryObject} this can be stored on
 */
public class QueryKey<T, Q extends IQueryObject<?, Q>> {

    private final String key;

    protected QueryKey(String key) {
        this.key = key;
    }

    public String key() {
        return this.key;
    }

    final List<T> createList() {
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public final Collection<T> getValues(Collection<Object> objects) {
        ImmutableList.Builder<T> builder = ImmutableList.builder();
        for (Object o : objects) {
            builder.add((T) o);
        }

        return builder.build();
    }

}
