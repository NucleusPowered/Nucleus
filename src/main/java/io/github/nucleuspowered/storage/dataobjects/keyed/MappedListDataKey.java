/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.dataobjects.keyed;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

import java.util.List;
import java.util.Map;

/**
 * A {@link Map} specific data key.
 *
 * @param <K> The key type of the {@link Map}.
 * @param <V> The list value type of the {@link Map}.
 * @param <O> The {@link IKeyedDataObject} this will apply to.
 */
public class MappedListDataKey<K, V, O extends IKeyedDataObject<?>> extends DataKeyImpl<Map<K, List<V>>, O>
        implements DataKey.MapListKey<K, V, O>{

    private final TypeToken<K> keyType;
    private final TypeToken<V> valueType;

    private static <Key, Value> TypeToken<Map<Key, List<Value>>> createMapListToken(
            TypeToken<Key> keyToken,
            TypeToken<Value> valueToken) {
        return new TypeToken<Map<Key, List<Value>>>() {}
                .where(new TypeParameter<Key>() {}, keyToken)
                .where(new TypeParameter<Value>() {}, valueToken);
    }

    public MappedListDataKey(String[] key, TypeToken<K> keyType, TypeToken<V> valueType, Class<O> target) {
        super(key, createMapListToken(keyType, valueType), target, null);
        this.keyType = keyType;
        this.valueType = valueType;
    }

}
