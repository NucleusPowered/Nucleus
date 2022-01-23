/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.impl.AbstractDataKey;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.impl.ListDataKey;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.impl.MappedDataKey;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.impl.MappedListDataKey;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.impl.ScalarDataKey;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Represents a data point in an {@link AbstractKeyBasedDataObject}
 *
 * @param <R> The type of object this translates to.
 * @param <O> The type of {@link IKeyedDataObject} that this can operate on
 */
public interface DataKey<R, O extends IKeyedDataObject<?>> {

    static <T, O extends IKeyedDataObject<?>> DataKey<T, O> of(final TypeToken<T> type, final Class<O> target, final String... key) {
        return new ScalarDataKey<>(key, type.getType(), target,  null);
    }

    static <T, O extends IKeyedDataObject<?>> DataKey<T, O> of(final T def, final TypeToken<T> type, final Class<O> target, final String... key) {
        return new ScalarDataKey<>(key, type.getType(), target, def);
    }

    static <T, O extends IKeyedDataObject<?>> DataKey.ListKey<T, O> ofList(final TypeToken<T> type, final Class<O> target, final String... key) {
        return new ListDataKey<>(key, type, target);
    }

    static <K, V, O extends IKeyedDataObject<?>> DataKey.MapKey<K, V, O> ofMap(
            final TypeToken<K> keyType, final TypeToken<V> value, final Class<O> target, final String... key) {
        return new MappedDataKey<>(key, keyType, value, target);
    }

    static <K, V, O extends IKeyedDataObject<?>> DataKey.MapListKey<K, V, O> ofMapList(
            final TypeToken<K> keyType, final TypeToken<V> listValueType, final Class<O> target, final String... key) {
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
    String[] getDataPath();

    /**
     * The {@link Class} of the data
     *
     * @return The {@link TypeToken}
     */
    Type getKeyType();

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
