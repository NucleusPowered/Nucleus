/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.dataobjects.keyed;

import io.github.nucleuspowered.storage.dataobjects.AbstractConfigurateBackedDataObject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.util.Optional;

import javax.annotation.Nullable;

public class AbstractKeyBasedDataObject<T extends IKeyedDataObject<T>> extends AbstractConfigurateBackedDataObject implements IKeyedDataObject<T> {

    @Override
    public boolean has(DataKey<?, ? extends T> dataKey) {
        return !getNode(dataKey.getKey()).isVirtual();
    }

    public <V> Value<V> getAndSet(DataKey<V, ? extends T> dataKey) {
        return new ValueImpl<>(getNullable(dataKey), dataKey);
    }

    @Nullable
    public <V> V getNullable(DataKey<V, ? extends T> dataKey) {
        try {
            return getNode(dataKey.getKey()).getValue(dataKey.getType());
        } catch (ObjectMappingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    public <V> V getOrDefault(DataKey<V, ? extends T> dataKey) {
        V t = getNullable(dataKey);
        if (t == null) {
            return dataKey.getDefault();
        }

        return t;
    }

    public <V> Optional<V> get(DataKey<V, ? extends T> dataKey) {
        return Optional.ofNullable(getNullable(dataKey));
    }

    public <V> boolean set(DataKey<V, ? extends T> dataKey, V data) {
        try {
            getNode(dataKey.getKey()).setValue(dataKey.getType(), data);
            return true;
        } catch (ObjectMappingException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void remove(DataKey<?, ? extends T> dataKey) {
        getNode(dataKey.getKey()).setValue(null);
    }

    private ConfigurationNode getNode(String[] key) {
        ConfigurationNode r = this.backingNode;
        for (String k : key) {
            r = r.getNode(k);
        }

        return r;
    }

    public class ValueImpl<V, B extends T> implements IKeyedDataObject.Value<V> {

        @Nullable private V value;
        private DataKey<V, B> dataKey;

        private ValueImpl(@Nullable V value, DataKey<V, B> dataKey) {
            this.value = value;
            this.dataKey = dataKey;
        }

        public Optional<V> getValue() {
            return Optional.ofNullable(this.value);
        }

        public void setValue(@Nullable V value) {
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
