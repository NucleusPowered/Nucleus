/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.impl;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.DataKey;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.IKeyedDataObject;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.leangen.geantyref.TypeToken;
import io.vavr.Tuple;
import io.vavr.control.Option;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A {@link Map} specific data key.
 *
 * @param <V> The value type of the {@link Map}.
 * @param <O> The {@link IKeyedDataObject} this will apply to.
 */
public class MappedDataKeyStringKeyed<V, O extends IKeyedDataObject<?>> extends AbstractDataKey<Map<String, V>, O>
        implements DataKey.StringKeyedMapKey<V, O> {

    private final Class<V> erasedType;

    private static <Value> Type createMapType(final TypeToken<Value> valueToken) {
        return TypeFactory.parameterizedClass(Map.class, String.class, valueToken.getType());
    }

    @SuppressWarnings("unchecked")
    public MappedDataKeyStringKeyed(final String[] key, final TypeToken<V> valueType, final Class<O> target, final @Nullable BiFunction<DataQuery, DataView, DataView> transformer) {
        super(key, MappedDataKeyStringKeyed.createMapType(valueType), target, null, transformer);
        this.erasedType = (Class<V>) GenericTypeReflector.erase(valueType.getType());
    }

    @Override
    public Optional<Map<String, V>> getFromDataView(final DataView dataView) {
        return dataView.getView(this.getDataQuery())
                .map(x ->
                        io.vavr.collection.List.ofAll(x.keys(false))
                            .flatMap(k -> Option.ofOptional(dataView.getView(k)).map(y -> Tuple.of(k, y)))
                            .flatMap(t -> Option.ofOptional(t._2.getObject(DataQuery.of(), this.erasedType))
                            .map(y -> Tuple.of(t._1.asString('.'), y)))
                            .toJavaMap(Function.identity()));
    }



}
