/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.datatypes;

import io.github.nucleuspowered.nucleus.api.util.data.NamedLocation;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.DataBuilder;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.persistence.Queries;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;

import java.text.MessageFormat;
import java.util.Optional;
import java.util.Vector;

public final class NucleusNamedLocation implements NamedLocation {

    public static final int NAMED_LOCATION_CONTENT_VERSION = 1;
    public static final DataQuery NAMED_LOCATION_DATA_QUERY = DataQuery.of("NamedLocation");

    private static final DataQuery nameKey = DataQuery.of("name");
    private static final DataQuery worldKey = DataQuery.of("world");
    private static final DataQuery positionKey = DataQuery.of("position");
    private static final DataQuery rotationKey = DataQuery.of("rotation");

    private final String name;
    private final ResourceKey worldResourceKey;
    private final Vector3d position;
    private final Vector3d rotation;

    public NucleusNamedLocation(final NamedLocation from) {
        this.name = from.getName();
        this.worldResourceKey = from.getWorldResourceKey();
        this.position = from.getPosition();
        this.rotation = from.getRotation();
    }

    public NucleusNamedLocation(final String name, final ResourceKey world, final Vector3d position, final Vector3d rotation) {
        this.rotation = rotation;
        this.position = position;
        this.name = name;
        this.worldResourceKey = world;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public ResourceKey getWorldResourceKey() {
        return this.worldResourceKey;
    }

    @Override public Optional<ServerWorld> getWorld() {
        return Sponge.server().worldManager().world(this.worldResourceKey);
    }

    public Vector3d getRotation() {
        return this.rotation;
    }

    @Override public Vector3d getPosition() {
        return this.position;
    }

    @Override public Optional<ServerLocation> getLocation() {
        return Sponge.server().worldManager().world(this.worldResourceKey).map(x -> x.location(this.position));
    }

    public String toLocationString() {
        return MessageFormat.format("name: {0}, world: {1}, x: {2}, y: {3}, z: {4}", this.name, this.worldResourceKey.asString(),
            (int) this.position.x(), (int) this.position.y(), (int) this.position.z());
    }

    @Override
    public int contentVersion() {
        return NucleusNamedLocation.NAMED_LOCATION_CONTENT_VERSION;
    }

    @Override
    public DataContainer toContainer() {
        return DataContainer.createNew()
                .set(Queries.CONTENT_VERSION, this.contentVersion())
                .set(NucleusNamedLocation.nameKey, this.name)
                .set(NucleusNamedLocation.worldKey, this.worldResourceKey)
                .set(NucleusNamedLocation.positionKey, this.position)
                .set(NucleusNamedLocation.rotationKey, this.rotation);
    }

    public static DataView upgradeLegacy(final DataView dataContainer, final DataQuery childQuery) {
        final DataView targetView = dataContainer.createView(childQuery);
        targetView.set(Queries.CONTENT_VERSION, 1);
        targetView.set(NucleusNamedLocation.nameKey, dataContainer.getString(DataQuery.of("name")));
        targetView.set(NucleusNamedLocation.worldKey, dataContainer.getResourceKey(DataQuery.of("world")));

        final Vector3d rotation = Vector3d.from(
                dataContainer.getDouble(DataQuery.of("rotx")).orElse(0.0),
                dataContainer.getDouble(DataQuery.of("roty")).orElse(0.0),
                dataContainer.getDouble(DataQuery.of("rotz")).orElse(0.0)
        );
        targetView.set(NucleusNamedLocation.rotationKey, rotation);

        final Vector3d position = Vector3d.from(
                dataContainer.getDouble(DataQuery.of("x")).orElse(0.0),
                dataContainer.getDouble(DataQuery.of("y")).orElse(0.0),
                dataContainer.getDouble(DataQuery.of("z")).orElse(0.0)
        );
        targetView.set(NucleusNamedLocation.positionKey, position);

        dataContainer.remove(DataQuery.of("name"))
            .remove(DataQuery.of("world"))
            .remove(DataQuery.of("x"))
            .remove(DataQuery.of("y"))
            .remove(DataQuery.of("z"))
            .remove(DataQuery.of("rotx"))
            .remove(DataQuery.of("roty"))
            .remove(DataQuery.of("rotz"));

        return dataContainer;
    }

    public static final class DataBuilder extends AbstractDataBuilder<NamedLocation> {

        public DataBuilder() {
            super(NamedLocation.class, NucleusNamedLocation.NAMED_LOCATION_CONTENT_VERSION);
        }

        @Override
        protected Optional<NamedLocation> buildContent(final DataView container) throws InvalidDataException {
            if (container.contains(NucleusNamedLocation.nameKey, NucleusNamedLocation.worldKey, NucleusNamedLocation.positionKey)) {
                return Optional.of(new NucleusNamedLocation(
                        container.getString(NucleusNamedLocation.nameKey).get(),
                        container.getResourceKey(NucleusNamedLocation.worldKey).get(),
                        container.getObject(NucleusNamedLocation.positionKey, Vector3d.class).get(),
                        container.getObject(NucleusNamedLocation.rotationKey, Vector3d.class).orElse(Vector3d.ZERO)
                ));
            }
            return Optional.empty();
        }
    }

}
