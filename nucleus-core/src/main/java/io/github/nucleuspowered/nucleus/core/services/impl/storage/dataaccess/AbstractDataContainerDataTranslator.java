/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.dataaccess;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IDataObject;
import io.github.nucleuspowered.storage.exceptions.DataLoadException;
import io.github.nucleuspowered.storage.exceptions.DataSaveException;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataContentUpdater;
import org.spongepowered.api.data.persistence.DataTranslator;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.persistence.Queries;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractDataContainerDataTranslator<R extends IDataObject> implements IDataTranslator<R, DataContainer>, DataTranslator<R> {

    private final int currentVersion;
    private final Collection<DataContentUpdater> updaters; // Only used for structural changes, not actual data changes (see DataVersioning for that)

    protected AbstractDataContainerDataTranslator(final int currentVersion, final Collection<DataContentUpdater> updaters) {
        this.currentVersion = currentVersion;
        this.updaters = updaters;
    }

    protected abstract R createNew(final DataView dataView);

    public int version() {
        return this.currentVersion;
    }

    @Override
    public final R fromDataAccessObject(final DataContainer object) throws DataLoadException {
        try {
            return this.translate(object);
        } catch (final InvalidDataException e) {
            throw new DataLoadException("Unable to load data: ", e);
        }
    }

    @Override
    public final DataContainer toDataAccessObject(final R object) throws DataSaveException {
        try {
            return this.translate(object);
        } catch (final InvalidDataException e) {
            throw new DataSaveException("Unable to save data: ", e);
        }
    }

    @Override
    public final R translate(final DataView view) throws InvalidDataException {
        final long version = view.getLong(Queries.CONTENT_VERSION).orElse(0L);
        DataView currentData = view;
        if (version != currentVersion) {
            final List<DataContentUpdater> updatersToRun = this.updaters.stream()
                    .filter(x -> x.inputVersion() >= currentVersion)
                    .sorted(Comparator.comparing(DataContentUpdater::inputVersion))
                    .collect(Collectors.toList());

            for (final DataContentUpdater updater : updatersToRun) {
                currentData = updater.update(currentData);
            }
        }
        return this.loadFromDataContainer(currentData);
    }

    @Override
    public final DataContainer translate(final R obj) throws InvalidDataException {
        final DataContainer container = this.saveToDataContainer(obj);
        container.set(Queries.CONTENT_VERSION, this.version());
        return container;
    }

    protected abstract R loadFromDataContainer(final DataView dataView) throws InvalidDataException;

    protected abstract DataContainer saveToDataContainer(final R obj) throws InvalidDataException;

}
