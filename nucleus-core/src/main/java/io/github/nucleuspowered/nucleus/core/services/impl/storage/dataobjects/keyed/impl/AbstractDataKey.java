/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.impl;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.DataKey;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.IKeyedDataObject;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Type;

public class AbstractDataKey<R, O extends IKeyedDataObject<?>> implements DataKey<R, O> {

    private final String[] key;
    private final Type type;
    private final R def;
    private final Class<O> target;

    public AbstractDataKey(final String[] key, final Type type, final Class<O> target, @Nullable final R def) {
        this.key = key;
        this.type = type;
        this.def = def;
        this.target = target;
    }

    @Override public Class<O> target() {
        return this.target;
    }

    @Override public String[] getDataPath() {
        return this.key;
    }

    @Override public Type getKeyType() {
        return this.type;
    }

    @Nullable @Override public R getDefault() {
        return this.def;
    }
}
