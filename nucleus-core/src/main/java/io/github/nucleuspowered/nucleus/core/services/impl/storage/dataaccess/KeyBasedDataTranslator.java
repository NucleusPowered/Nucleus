/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.dataaccess;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.AbstractKeyBasedDataObject;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.IKeyedDataObject;
import io.leangen.geantyref.TypeToken;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;

import java.util.Collections;
import java.util.function.Function;

public class KeyBasedDataTranslator<K extends IKeyedDataObject<K>, T extends AbstractKeyBasedDataObject<K>> extends AbstractDataContainerDataTranslator<K> {

    private final TypeToken<K> typeToken;
    private final Function<DataView, K> create;

    public KeyBasedDataTranslator(final TypeToken<K> typeToken, final Function<DataView, K> create) {
        super(1, Collections.emptyList());
        this.typeToken = typeToken;
        this.create = create;
    }

    @Override
    protected K loadFromDataContainer(final DataView dataView) throws InvalidDataException {
        return this.create.apply(dataView);
    }

    @Override
    protected DataContainer saveToDataContainer(final K obj) throws InvalidDataException {
        if (obj instanceof AbstractKeyBasedDataObject<?>) {
            return ((AbstractKeyBasedDataObject<?>) obj).data().copy();
        }
        throw new InvalidDataException(obj.getClass().getName() + "is not of type AbstractKeyBasedDataObject");
    }

    @Override
    public K createNew() {
        return this.create.apply(DataContainer.createNew());
    }

    @Override
    protected K createNew(final DataView dataView) {
        return this.create.apply(dataView);
    }

    @Override
    public TypeToken<K> token() {
        return this.typeToken;
    }
}
