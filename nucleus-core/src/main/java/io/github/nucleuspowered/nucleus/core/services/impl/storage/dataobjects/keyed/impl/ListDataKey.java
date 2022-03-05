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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A {@link List} specific data key.
 *
 * @param <R> The inner type of the {@link List}.
 * @param <O> The {@link IKeyedDataObject} this will apply to.
 */
public class ListDataKey<R, O extends IKeyedDataObject<?>> extends AbstractDataKey<List<R>, O>
        implements DataKey.ListKey<R, O> {

    private final TypeToken<R> innerType;

    private static <S> Type createListToken(final TypeToken<S> innerToken) {
        return TypeFactory.parameterizedClass(List.class, innerToken.getType());
    }

    public ListDataKey(final String[] key, final TypeToken<R> type, final Class<O> target, final @Nullable BiFunction<DataQuery, DataView, DataView> transformer) {
        super(key, ListDataKey.createListToken(type), target, null, transformer);
        this.innerType = type;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<List<R>> getFromDataView(final DataView view) {
        return view.getObjectList(this.getDataQuery(), (Class<R>) GenericTypeReflector.erase(this.innerType.getType()));
    }
}
