/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.dataobjects.keyed;

import com.google.common.reflect.TypeToken;

import javax.annotation.Nullable;

public class DataKeyImpl<R, O extends IKeyedDataObject<?>> implements DataKey<R, O> {

    private final String[] key;
    private final Object[] objectKey;
    private final TypeToken<R> type;
    private final R def;
    private Class<O> target;

    public DataKeyImpl(String[] key, TypeToken<R> type, Class<O> target, @Nullable R def) {
        this.key = key;
        this.objectKey = new Object[key.length];
        System.arraycopy(this.key, 0, this.objectKey, 0, key.length);
        this.type = type;
        this.def = def;
        this.target = target;
    }

    @Override public Class<O> target() {
        return this.target;
    }

    @Override public String[] getKey() {
        return this.key;
    }

    @Override public Object[] getObjectArrayKey() {
        return this.objectKey;
    }

    @Override public TypeToken<R> getType() {
        return this.type;
    }

    @Nullable @Override public R getDefault() {
        return this.def;
    }
}
