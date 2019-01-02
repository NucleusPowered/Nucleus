/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.dataobjects.keyed;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

import java.util.Map;

/**
 * A {@link Map} specific data key.
 *
 * @param <K> The key type of the {@link Map}.
 * @param <V> The value type of the {@link Map}.
 * @param <O> The {@link IKeyedDataObject} this will apply to.
 */
public class MappedDataKey<K, V, O extends IKeyedDataObject<?>> extends DataKeyImpl<Map<K, V>, O>
        implements DataKey.MapKey<K, V, O>{

    private final TypeToken<K> keyType;
    private final TypeToken<V> valueType;

    private static <Key, Value> TypeToken<Map<Key, Value>> createMapToken(TypeToken<Key> keyToken, TypeToken<Value> valueToken) {
        return new TypeToken<Map<Key, Value>>() {}
                .where(new TypeParameter<Key>() {}, keyToken)
                .where(new TypeParameter<Value>() {}, valueToken);
    }

    public MappedDataKey(String[] key, TypeToken<K> keyType, TypeToken<V> valueType, Class<O> target) {
        super(key, createMapToken(keyType, valueType), target, null);
        this.keyType = keyType;
        this.valueType = valueType;
    }

}
