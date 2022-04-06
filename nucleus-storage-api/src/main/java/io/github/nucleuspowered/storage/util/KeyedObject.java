/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.util;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.function.Function;

public class KeyedObject<K, O> {

    private final K key;
    @Nullable private final O value;

    public KeyedObject(final K key, @Nullable final O value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return this.key;
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
    public <R> KeyedObject<K, R> mapValue(final Function<O, R> mapper) {
        return new KeyedObject<>(this.key, this.value != null ? mapper.apply(this.value) : null);
    }
}
