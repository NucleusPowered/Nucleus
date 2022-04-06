/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.data;

import io.github.nucleuspowered.nucleus.api.module.warp.data.Warp;
import io.github.nucleuspowered.nucleus.api.util.data.NamedLocation;
import io.github.nucleuspowered.nucleus.core.datatypes.NucleusNamedLocation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.persistence.Queries;
import org.spongepowered.math.vector.Vector3d;

import javax.xml.crypto.Data;
import java.util.ConcurrentModificationException;
import java.util.Optional;

public class NucleusWarp implements Warp {

    public static final int CONTENT_VERSION = 1;
    private static final DataQuery CATEGORY = DataQuery.of("category");
    private static final DataQuery COST = DataQuery.of("cost");
    private static final DataQuery DESCRIPTION = DataQuery.of("description");

    private final String category;
    private final Double cost;
    private final Component description;
    private final NamedLocation namedLocation;

    public NucleusWarp(final String category,
                       final double cost,
                       final Component description,
                       final NamedLocation namedLocation) {
        this.category = category;
        this.cost = cost == 0 ? null : cost;
        this.description = description;
        this.namedLocation = namedLocation;
    }

    @Override
    public NamedLocation getNamedLocation() {
        return this.namedLocation;
    }

    @Override
    public Optional<String> getCategory() {
        return Optional.ofNullable(this.category);
    }

    @Override
    public Optional<Double> getCost() {
        return Optional.ofNullable(this.cost);
    }

    @Override
    public Optional<Component> getDescription() {
        return Optional.ofNullable(this.description);
    }

    @Override
    public int contentVersion() {
        return NucleusWarp.CONTENT_VERSION;
    }

    @Override
    public DataContainer toContainer() {
        final DataContainer container = DataContainer.createNew()
                .set(NucleusNamedLocation.NAMED_LOCATION_DATA_QUERY, this.namedLocation);
        this.getCategory().ifPresent(x -> container.set(NucleusWarp.CATEGORY, x));
        this.getCost().ifPresent(x -> container.set(NucleusWarp.COST, x));
        this.getDescription().ifPresent(x -> container.set(NucleusWarp.DESCRIPTION, x));
        return container;
    }

    public static final class DataBuilder extends AbstractDataBuilder<Warp> {

        public DataBuilder() {
            super(Warp.class, NucleusWarp.CONTENT_VERSION);
        }

        @Override
        protected Optional<Warp> buildContent(final DataView container) throws InvalidDataException {
            if (!container.contains(Queries.CONTENT_VERSION)) {
                NucleusNamedLocation.upgradeLegacy(container, DataQuery.of());
                // we used the Gson serialiser before, we need to go to the DataContainer one.
                if (container.contains(NucleusWarp.DESCRIPTION)) {
                    final Component component = GsonComponentSerializer.gson().deserialize(container.getString(NucleusWarp.DESCRIPTION).get());
                    container.set(NucleusWarp.DESCRIPTION, component);
                }
            }

            if (container.contains(NucleusNamedLocation.NAMED_LOCATION_DATA_QUERY)) {
                return Optional.of(
                        new NucleusWarp(
                                container.getString(NucleusWarp.CATEGORY).orElse(null),
                                container.getDouble(NucleusWarp.COST).orElse(0.0),
                                container.getObject(NucleusWarp.DESCRIPTION, Component.class).orElse(null),
                                container.getObject(NucleusNamedLocation.NAMED_LOCATION_DATA_QUERY, NamedLocation.class).get()
                        )
                );
            }
            return Optional.empty();
        }
    }

}
