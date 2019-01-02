/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.dataobjects.keyed;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

import java.util.List;

/**
 * A {@link List} specific data key.
 *
 * @param <R> The inner type of the {@link List}.
 * @param <O> The {@link IKeyedDataObject} this will apply to.
 */
public class ListDataKey<R, O extends IKeyedDataObject<?>> extends DataKeyImpl<List<R>, O>
        implements DataKey.ListKey<R, O>{

    private final TypeToken<R> innerType;

    private static <S> TypeToken<List<S>> createListToken(TypeToken<S> innerToken) {
        return new TypeToken<List<S>>() {}.where(new TypeParameter<S>() {}, innerToken);
    }

    public ListDataKey(String[] key, TypeToken<R> type, Class<O> target) {
        super(key, createListToken(type), target, null);
        this.innerType = type;
    }

}
