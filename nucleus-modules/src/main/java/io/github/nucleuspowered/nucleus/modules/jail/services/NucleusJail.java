/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.services;

import io.github.nucleuspowered.nucleus.api.module.jail.data.Jail;
import io.github.nucleuspowered.nucleus.api.util.data.NamedLocation;
import io.github.nucleuspowered.nucleus.core.datatypes.NucleusNamedLocation;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.persistence.Queries;

import java.util.Optional;

import static io.github.nucleuspowered.nucleus.core.datatypes.NucleusNamedLocation.NAMED_LOCATION_DATA_QUERY;

public final class NucleusJail implements Jail {

    public static final int CONTENT_VERSION = 1;

    private final NamedLocation namedLocation;

    public NucleusJail(final NamedLocation namedLocation) {
        this.namedLocation = namedLocation;
    }

    @Override
    public int contentVersion() {
        return NucleusJail.CONTENT_VERSION;
    }

    @Override
    public DataContainer toContainer() {
        return DataContainer.createNew()
                .set(NucleusNamedLocation.NAMED_LOCATION_DATA_QUERY, this.namedLocation);
    }

    @Override
    public NamedLocation getLocation() {
        return this.namedLocation;
    }

    public static final class DataBuilder extends AbstractDataBuilder<Jail> {

        public DataBuilder() {
            super(Jail.class, NucleusJail.CONTENT_VERSION);
        }

        @Override
        protected Optional<Jail> buildContent(final DataView container) throws InvalidDataException {
            DataView dataView = container;
            if (!container.contains(Queries.CONTENT_VERSION)) {
                NucleusNamedLocation.upgradeLegacy(dataView, NucleusNamedLocation.NAMED_LOCATION_DATA_QUERY);
                container.set(Queries.CONTENT_VERSION, NucleusJail.CONTENT_VERSION);
            }
            return dataView.getObject(NucleusNamedLocation.NAMED_LOCATION_DATA_QUERY, NamedLocation.class).map(NucleusJail::new);
        }
    }

}
