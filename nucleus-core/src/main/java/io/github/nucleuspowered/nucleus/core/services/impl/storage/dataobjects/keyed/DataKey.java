/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.impl.ListDataKey;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.impl.MappedDataKeyStringKeyed;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.impl.StringKeyedMappedListDataKeyStringKeyed;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.impl.ScalarDataKey;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a data point in an {@link AbstractKeyBasedDataObject}
 *
 * @param <R> The type of object this translates to.
 * @param <O> The type of {@link IKeyedDataObject} that this can operate on
 */
public interface DataKey<R, O extends IKeyedDataObject<?>> {

    static <T, O extends IKeyedDataObject<?>> DataKey<T, O> of(final TypeToken<T> type, final Class<O> target, final String... key) {
        return new ScalarDataKey<>(key, type.getType(), target,  null, null);
    }

    static <T, O extends IKeyedDataObject<?>> DataKey<T, O> of(final T def, final TypeToken<T> type, final Class<O> target, final String... key) {
        return new ScalarDataKey<>(key, type.getType(), target, def, null);
    }

    static <T, O extends IKeyedDataObject<?>> DataKey.ListKey<T, O> ofList(final TypeToken<T> type, final Class<O> target, final BiFunction<DataQuery, DataView, DataView> transformer, final String... key) {
        return new ListDataKey<>(key, type, target, transformer);
    }

    static <T, O extends IKeyedDataObject<?>> DataKey.ListKey<T, O> ofList(final TypeToken<T> type, final Class<O> target, final String... key) {
        return new ListDataKey<>(key, type, target, null);
    }

    static <V, O extends IKeyedDataObject<?>> StringKeyedMapKey<V, O> ofMap(final TypeToken<V> value, final Class<O> target, final String... key) {
        return new MappedDataKeyStringKeyed<>(key, value, target, null);
    }

    static <K, V, O extends IKeyedDataObject<?>> StringKeyedMapListKey<V, O> ofMapList(
            final TypeToken<V> listValueType, final Class<O> target, final String... key) {
        return new StringKeyedMappedListDataKeyStringKeyed<>(key, listValueType, target, null);
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

    DataQuery getDataQuery();

    /**
     * The {@link Class} of the data
     *
     * @return The {@link TypeToken}
     */
    Type getKeyType();

    /**
     * Gets the value from the provided {@link DataView}, if it exists
     * when queried with the given {@link DataQuery}
     *
     * @param view The view
     * @return The value, if it exists
     */
    Optional<R> getFromDataView(DataView view);

    /**
     * The default
     *
     * @return The default
     */
    @Nullable R getDefault();

    void performTransformation(DataContainer data);

    interface ListKey<R, O extends IKeyedDataObject<?>> extends DataKey<List<R>, O> { }

    interface StringKeyedMapKey<V, O extends IKeyedDataObject<?>> extends DataKey<Map<String, V>, O> { }

    interface StringKeyedMapListKey<V, O extends IKeyedDataObject<?>> extends DataKey<Map<String, List<V>>, O> { }
}
