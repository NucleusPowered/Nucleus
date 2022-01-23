/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.impl;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.DataKey;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.IKeyedDataObject;
import io.leangen.geantyref.TypeFactory;
import io.leangen.geantyref.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * A {@link Map} specific data key.
 *
 * @param <K> The key type of the {@link Map}.
 * @param <V> The list value type of the {@link Map}.
 * @param <O> The {@link IKeyedDataObject} this will apply to.
 */
public class MappedListDataKey<K, V, O extends IKeyedDataObject<?>> extends AbstractDataKey<Map<K, List<V>>, O>
        implements DataKey.MapListKey<K, V, O> {

    private final TypeToken<K> keyType;
    private final TypeToken<V> valueType;

    private static <Key, Value> Type createMapListToken(
            final TypeToken<Key> keyToken,
            final TypeToken<Value> valueToken) {
        return TypeFactory.parameterizedClass(Map.class, keyToken.getType(), TypeFactory.parameterizedClass(List.class, valueToken.getType()));
    }

    public MappedListDataKey(final String[] key, final TypeToken<K> keyType, final TypeToken<V> valueType, final Class<O> target) {
        super(key, createMapListToken(keyType, valueType), target, null);
        this.keyType = keyType;
        this.valueType = valueType;
    }

}
