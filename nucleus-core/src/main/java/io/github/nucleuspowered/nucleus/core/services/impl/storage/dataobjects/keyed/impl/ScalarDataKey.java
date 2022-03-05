/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.impl;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.IKeyedDataObject;
import io.leangen.geantyref.GenericTypeReflector;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class ScalarDataKey<R, O extends IKeyedDataObject<?>> extends AbstractDataKey<R, O> {

    public ScalarDataKey(final String[] key, final Type type, final Class<O> target, @Nullable final R def, final @Nullable BiFunction<DataQuery, DataView, DataView> transformer) {
        super(key, type, target, def, transformer);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<R> getFromDataView(final DataView view) {
        return view.getObject(this.getDataQuery(), (Class<R>) GenericTypeReflector.erase(this.getKeyType()));
    }
}
