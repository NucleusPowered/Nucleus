/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.storage;

import io.github.nucleuspowered.nucleus.api.module.kit.data.Kit;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataaccess.AbstractDataContainerDataTranslator;
import io.leangen.geantyref.TypeToken;
import io.vavr.Tuple;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class KitDataTranslator extends AbstractDataContainerDataTranslator<IKitDataObject> {

    public final static class Holder {
        public static final KitDataTranslator INSTANCE = new KitDataTranslator();
    }

    private KitDataTranslator() {
        super(1, Collections.emptyList());
    }

    @Override
    public IKitDataObject createNew() {
        return new KitDataObject();
    }

    @Override
    public TypeToken<IKitDataObject> token() {
        return TypeToken.get(IKitDataObject.class);
    }

    @Override
    protected IKitDataObject createNew(final DataView dataView) {
        return this.loadFromDataContainer(dataView);
    }

    @Override
    protected IKitDataObject loadFromDataContainer(final DataView dataView) throws InvalidDataException {
        final IKitDataObject dataObject = this.createNew();
        dataObject
                .setKitMap(dataView.keys(false)
                .stream()
                .map(query -> dataView.getObject(query, Kit.class).map(x -> Tuple.of(query.parts().get(0), x)).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(k -> k._1, k -> k._2)));
        return dataObject;
    }

    @Override
    public DataContainer saveToDataContainer(final IKitDataObject obj) throws InvalidDataException {
        DataContainer container = DataContainer.createNew();
        for (final Map.Entry<String, Kit> kit : obj.getKitMap().entrySet()) {
            container = container.set(DataQuery.of(kit.getKey()), kit.getValue());
        }
        return container;
    }

}
