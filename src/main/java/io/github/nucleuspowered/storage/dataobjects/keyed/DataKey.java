/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.dataobjects.keyed;

import com.google.common.reflect.TypeToken;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Represents a data point in an {@link AbstractKeyBasedDataObject}
 *
 * @param <R> The type of object this translates to.
 * @param <O> The type of {@link IKeyedDataObject} that this can operate on
 */
public interface DataKey<R, O extends IKeyedDataObject<?>> {

    static <T, O extends IKeyedDataObject<?>> DataKey<T, O> of(TypeToken<T> type, Class<O> target, String... key) {
        return new DataKeyImpl<>(key, type, target,  null);
    }

    static <T, O extends IKeyedDataObject<?>> DataKey<T, O> of(T def, TypeToken<T> type, Class<O> target, String... key) {
        return new DataKeyImpl<>(key, type, target, def);
    }

    static <T, O extends IKeyedDataObject<?>> DataKey.ListKey<T, O> ofList(TypeToken<T> type, Class<O> target, String... key) {
        return new ListDataKey<>(key, type, target);
    }

    static <K, V, O extends IKeyedDataObject<?>> DataKey.MapKey<K, V, O> ofMap(
            TypeToken<K> keyType, TypeToken<V> value, Class<O> target, String... key) {
        return new MappedDataKey<>(key, keyType, value, target);
    }

    static <K, V, O extends IKeyedDataObject<?>> DataKey.MapListKey<K, V, O> ofMapList(
            TypeToken<K> keyType, TypeToken<V> listValueType, Class<O> target, String... key) {
        return new MappedListDataKey<>(key, keyType, listValueType, target);
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

    interface ListKey<R, O extends IKeyedDataObject<?>> extends DataKey<List<R>, O> { }

    interface MapKey<K, V, O extends IKeyedDataObject<?>> extends DataKey<Map<K, V>, O> { }

    interface MapListKey<K, V, O extends IKeyedDataObject<?>> extends DataKey<Map<K, List<V>>, O> { }
}
