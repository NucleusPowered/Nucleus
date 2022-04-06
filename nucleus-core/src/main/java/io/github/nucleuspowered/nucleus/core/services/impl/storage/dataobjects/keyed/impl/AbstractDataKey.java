/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.impl;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.DataKey;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.IKeyedDataObject;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;

import java.lang.reflect.Type;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractDataKey<R, O extends IKeyedDataObject<?>> implements DataKey<R, O> {

    private final String[] key;
    private final DataQuery dataQuery;
    private final Type type;
    private final R def;
    private final Class<O> target;
    private final BiFunction<DataQuery, DataView, DataView> transform;

    public AbstractDataKey(final String[] key, final Type type, final Class<O> target, @Nullable final R def, final @Nullable BiFunction<DataQuery, DataView, DataView> transform) {
        this.key = key;
        this.type = type;
        this.def = def;
        this.target = target;
        this.dataQuery = DataQuery.of(key);
        this.transform = transform;
    }

    @Override public Class<O> target() {
        return this.target;
    }

    @Override public String[] getDataPath() {
        return this.key;
    }

    @Override
    public final DataQuery getDataQuery() {
        return this.dataQuery;
    }

    @Override public Type getKeyType() {
        return this.type;
    }

    @Nullable @Override public R getDefault() {
        return this.def;
    }

    @Override
    public final void performTransformation(final DataContainer data) {
        if (this.transform != null && data.contains(this.dataQuery)) {
            data.set(this.dataQuery, this.transform.apply(this.dataQuery, data));
        }
    }
}
