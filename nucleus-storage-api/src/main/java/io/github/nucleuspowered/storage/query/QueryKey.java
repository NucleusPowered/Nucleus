/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A key used for setting up queries.
 *
 * @param <T> The type of object the key is associated with
 * @param <Q> The {@link IQueryObject} this can be stored on
 */
public class QueryKey<T, Q extends IQueryObject<?, Q>> {

    private final String key;

    protected QueryKey(final String key) {
        this.key = key;
    }

    public String key() {
        return this.key;
    }

    public final List<T> createList() {
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public final Collection<T> getValues(final Collection<Object> objects) {
        final ArrayList<T> builder = new ArrayList<>();
        for (final Object o : objects) {
            builder.add((T) o);
        }

        return Collections.unmodifiableList(builder);
    }

}
