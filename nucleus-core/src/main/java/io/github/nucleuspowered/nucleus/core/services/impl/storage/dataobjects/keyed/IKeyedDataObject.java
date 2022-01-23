/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IDataObject;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

public interface IKeyedDataObject<T extends IKeyedDataObject<T>> extends IDataObject {

    default boolean has(final DataKey<?, ? extends T> dataKey) {
        return this.get(dataKey).isPresent();
    }

    <T2> Value<T2> getAndSet(DataKey<T2, ? extends T> dataKey);

    @Nullable <T2> T2 getNullable(DataKey<T2, ? extends T> dataKey);

    @Nullable <T2> T2 getOrDefault(DataKey<T2, ? extends T> dataKey);

    <T2> Optional<T2> get(DataKey<T2, ? extends T> dataKey);

    <T2> boolean set(DataKey<T2, ? extends T> dataKey, T2 data);

    void remove(DataKey<?, ? extends T> dataKey);

    boolean isDirty();

    default void markDirty() {
        this.markDirty(true);
    }

    void markDirty(boolean dirty);

    interface Value<T> extends AutoCloseable {

        Optional<T> getValue();

        void setValue(@Nullable T value);

        @Override void close();
    }

}
