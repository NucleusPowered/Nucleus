/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.dataobjects.keyed;

import io.github.nucleuspowered.storage.dataobjects.IDataObject;

import java.util.Optional;

import javax.annotation.Nullable;

public interface IKeyedDataObject<T extends IKeyedDataObject<T>> extends IDataObject {

    <T2> Value<T2> getAndSet(DataKey<T2, ? extends T> dataKey);

    @Nullable <T2> T2 getNullable(DataKey<T2, ? extends T> dataKey);

    @Nullable <T2> T2 getOrDefault(DataKey<T2, ? extends T> dataKey);

    <T2> Optional<T2> get(DataKey<T2, ? extends T> dataKey);

    <T2> boolean set(DataKey<T2, ? extends T> dataKey, T2 data);

    void remove(DataKey<?, ? extends T> dataKey);

    interface Value<T> extends AutoCloseable {

        Optional<T> getValue();

        void setValue(@Nullable T value);

        @Override void close();
    }

}
