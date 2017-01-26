/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.spongedata.manipulators;

import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.DataManipulatorBuilder;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Optional;

@NonnullByDefault
public abstract class AbstractSignDataBuilder<B extends AbstractSignManipulator<B, I>, I extends AbstractImmutableSignManipulator<I, B>> extends AbstractDataBuilder<B>
    implements DataManipulatorBuilder<B, I> {

    protected AbstractSignDataBuilder(Class<B> requiredClass, int supportedVersion) {
        super(requiredClass, supportedVersion);
    }

    @Override protected Optional<B> buildContent(DataView container) throws InvalidDataException {
        return create().from(container.getContainer());
    }

    @Override public abstract B create();

    @Override public Optional<B> createFrom(DataHolder dataHolder) {
        return create().fill(dataHolder);
    }
}
