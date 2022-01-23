package io.github.nucleuspowered.nucleus.core.services.impl.storage.dataaccess;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.AbstractKeyBasedDataObject;
import io.leangen.geantyref.TypeToken;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;

import java.util.Collections;
import java.util.function.Supplier;

public class KeyBasedDataTranslator<T extends AbstractKeyBasedDataObject<T>> extends AbstractDataContainerDataTranslator<T> {

    private final TypeToken<T> typeToken;
    private final Supplier<T> create;

    public KeyBasedDataTranslator(final TypeToken<T> typeToken, final Supplier<T> create) {
        super(1, Collections.emptyList());
        this.typeToken = typeToken;
        this.create = create;
    }

    @Override
    protected T translateCurrentVersion(final DataView dataView) throws InvalidDataException {
        // for each data query, if there is a matching key, apply it to the map if it's of the right type,
        // else put it in invalid.

        return null;
    }

    @Override
    protected DataContainer translateCurrentVersion(final T obj, final DataContainer dataView) throws InvalidDataException {
        return null;
    }


    @Override
    public T createNew() {
        return this.create.get();
    }

    @Override
    public TypeToken<T> token() {
        return this.typeToken;
    }
}
