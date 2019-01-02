/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.dataobjects.keyed;

import com.google.common.reflect.TypeToken;

import javax.annotation.Nullable;

/**
 * Represents a data point in an {@link AbstractKeyBasedDataObject}
 *
 * @param <R> The type of object this translates to.
 * @param <O> The type of {@link IKeyedDataObject} that this can operate on
 */
public interface DataKey<R, O extends IKeyedDataObject> {

    static <T, O extends IKeyedDataObject> DataKey<T, O> of(TypeToken<T> type, Class<O> target, String... key) {
        return new DataKeyImpl<>(key, type, target,  null);
    }

    static <T, O extends IKeyedDataObject> DataKey<T, O> of(T def, TypeToken<T> type, Class<O> target, String... key) {
        return new DataKeyImpl<>(key, type, target, def);
    }

    /**
     * The class of the {@link IKeyedDataObject} that this targets
     *
     * @return The class
     */
    Class<O> target();

    /**
     * The path to the data.
     *
     * @return The key
     */
    String[] getKey();

    /**
     * The {@link Class} of the data
     *
     * @return The {@link TypeToken}
     */
    TypeToken<R> getType();

    /**
     * The default
     *
     * @return The default
     */
    @Nullable R getDefault();

}
