/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.data;

import io.github.nucleuspowered.nucleus.api.module.warp.data.WarpCategory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.persistence.Queries;

import java.util.Optional;

public class NucleusWarpCategory implements WarpCategory {

    public static final int CONTENT_VERSION = 1;
    private static final DataQuery DISPLAY_NAME = DataQuery.of("displayName");
    private static final DataQuery DESCRIPTION = DataQuery.of("description");
    public static final DataQuery ID = DataQuery.of("id");

    public NucleusWarpCategory(final String id, final Component displayName, final @Nullable Component description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    private final String id;
    private final Component displayName;
    private final @Nullable Component description;

    @Override
    public Component getDisplayName() {
        return this.displayName;
    }

    @Override
    public Optional<Component> getDescription() {
        return Optional.ofNullable(this.description);
    }

    @Override
    public int contentVersion() {
        return NucleusWarpCategory.CONTENT_VERSION;
    }

    @Override
    public DataContainer toContainer() {
        final DataContainer dataContainer = DataContainer.createNew()
                .set(NucleusWarpCategory.ID, this.id)
                .set(NucleusWarpCategory.DISPLAY_NAME, this.displayName);
        if (description != null) {
            dataContainer.set(NucleusWarpCategory.DESCRIPTION, this.description);
        }
        return dataContainer;
    }

    @Override
    public String getId() {
        return this.id;
    }

    public static final class DataBuilder extends AbstractDataBuilder<WarpCategory> {
        public DataBuilder() {
            super(WarpCategory.class, NucleusWarpCategory.CONTENT_VERSION);
        }

        @Override
        protected Optional<WarpCategory> buildContent(final DataView container) throws InvalidDataException {
            if (!container.contains(Queries.CONTENT_VERSION)) {
                if (container.contains(NucleusWarpCategory.DESCRIPTION)) {
                    final Component component = GsonComponentSerializer.gson().deserialize(container.getString(NucleusWarpCategory.DESCRIPTION).get());
                    container.set(NucleusWarpCategory.DESCRIPTION, component);
                }
                final Component component = container.getString(NucleusWarpCategory.DISPLAY_NAME)
                        .map(x -> GsonComponentSerializer.gson().deserialize(x))
                        .orElseGet(() -> Component.text(container.getString(NucleusWarpCategory.ID).get()));
                container.set(NucleusWarpCategory.DISPLAY_NAME, component);
            }
            if (container.contains(NucleusWarpCategory.DISPLAY_NAME, NucleusWarpCategory.ID)) {
                return Optional.of(
                        new NucleusWarpCategory(
                                container.getString(NucleusWarpCategory.ID).get(),
                                container.getObject(NucleusWarpCategory.DISPLAY_NAME, Component.class).get(),
                                container.getObject(NucleusWarpCategory.DESCRIPTION, Component.class).orElse(null)
                        )
                );
            }
            return Optional.empty();
        }
    }

}
