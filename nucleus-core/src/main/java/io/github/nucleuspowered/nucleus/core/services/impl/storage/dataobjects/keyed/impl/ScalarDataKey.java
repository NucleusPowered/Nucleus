package io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.impl;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.IKeyedDataObject;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Type;

public class ScalarDataKey<R, O extends IKeyedDataObject<?>> extends AbstractDataKey<R, O> {

    public ScalarDataKey(final String[] key, final Type type, final Class<O> target, @Nullable final R def) {
        super(key, type, target, def);
    }

}
