/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractKeyBasedDataObject<T extends IKeyedDataObject<T>> implements IKeyedDataObject<T> {

    private transient boolean isDirty = false;

    private final Map<DataKey<?, ? extends T>, Object> dataHolder = new HashMap<>();

    private final DataContainer data;

    protected AbstractKeyBasedDataObject() {
        this.data = DataContainer.createNew();
    }

    protected AbstractKeyBasedDataObject(final DataView view) {
        this.data = view.copy(DataView.SafetyMode.NO_DATA_CLONED);
    }

    public final DataContainer data() {
        return this.data;
    }

    @Override
    public final void markDirty() {
        this.isDirty = true;
    }

    @Override
    public final void markDirty(final boolean markDirty) {
        this.isDirty = markDirty;
    }

    @Override
    public final boolean isDirty() {
        return this.isDirty;
    }

    @Override
    public boolean has(final DataKey<?, ? extends T> dataKey) {
        return this.dataHolder.containsKey(dataKey);
    }

    public <V> Value<V> getAndSet(final DataKey<V, ? extends T> dataKey) {
        return new ValueImpl<>(this.getNullable(dataKey), dataKey);
    }

    @Nullable
    public <V> V getNullable(final DataKey<V, ? extends T> dataKey) {
        return get(dataKey).orElse(null);
    }

    @Nullable
    public <V> V getOrDefault(final DataKey<V, ? extends T> dataKey) {
        final V t = this.getNullable(dataKey);
        if (t == null) {
            return dataKey.getDefault();
        }

        return t;
    }

    @SuppressWarnings("unchecked")
    public <V> Optional<V> get(final DataKey<V, ? extends T> dataKey) {
        if (this.dataHolder.containsKey(dataKey)) { // might have a null value, can't just assume no value needs to hit the container
            return Optional.ofNullable((V) this.dataHolder.get(dataKey));
        }
        dataKey.performTransformation(this.data);
        final Optional<V> v = dataKey.getFromDataView(this.data);
        this.dataHolder.put(dataKey, v.orElse(null));
        return v;
    }

    public <V> boolean set(final DataKey<V, ? extends T> dataKey, final V data) {

        this.dataHolder.put(dataKey, data);
        this.markDirty();
        return true;
    }

    public void remove(final DataKey<?, ? extends T> dataKey) {
        this.dataHolder.remove(dataKey);
        this.markDirty();
    }

    public final class ValueImpl<V, B extends T> implements IKeyedDataObject.Value<V> {

        @Nullable private V value;
        private final DataKey<V, B> dataKey;

        private ValueImpl(@Nullable final V value, final DataKey<V, B> dataKey) {
            this.value = value;
            this.dataKey = dataKey;
        }

        public Optional<V> getValue() {
            return Optional.ofNullable(this.value);
        }

        public void setValue(@Nullable final V value) {
            this.value = value;
        }

        @Override
        public void close() {
            if (this.value == null) {
                AbstractKeyBasedDataObject.this.remove(this.dataKey);
            } else {
                AbstractKeyBasedDataObject.this.set(this.dataKey, this.value);
            }
        }
    }
}
