/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.spongedata.warp;

import io.github.nucleuspowered.nucleus.api.spongedata.warp.ImmutableWarpSignData;
import io.github.nucleuspowered.nucleus.api.spongedata.warp.WarpSignData;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.DataManipulatorBuilder;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.InvalidDataException;

import java.util.Optional;

public class NucleusWarpSignDataManipulatorBuilder extends AbstractDataBuilder<WarpSignData> implements DataManipulatorBuilder<WarpSignData, ImmutableWarpSignData> {

    public NucleusWarpSignDataManipulatorBuilder() {
        super(WarpSignData.class, 1);
    }

    @Override
    protected Optional<WarpSignData> buildContent(DataView container) throws InvalidDataException {
        // There seriously isn't any point to me doing that again...
        return new NucleusWarpSignData().from(container.getContainer());
    }

    @Override
    public WarpSignData create() {
        return new NucleusWarpSignData();
    }

    @Override
    public Optional<WarpSignData> createFrom(DataHolder dataHolder) {
        return Optional.of(dataHolder.get(WarpSignData.class).orElse(create()));
    }
}
