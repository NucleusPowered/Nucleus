/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.util;

import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nullable;

public class KeyedObject<K, O> {

    private final K key;
    @Nullable private final O value;

    public KeyedObject(K key, @Nullable O value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return this.key;
    }

    public boolean hasValue() {
        return this.value != null;
    }

    public Optional<O> getValue() {
        return Optional.ofNullable(this.value);
    }

    /**
     * Transforms the value from type {@link O} to type {@link R}.
     *
     * <p>If the value is null, the output will be null.</p>
     *
     * @param mapper The mapper
     * @param <R> The type to transform to
     * @return The new {@link KeyedObject}s.
     */
    public <R> KeyedObject<K, R> mapValue(Function<O, R> mapper) {
        return new KeyedObject<>(this.key, this.value != null ? mapper.apply(this.value) : null);
    }
}
