/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.spongedata;

import io.github.nucleuspowered.nucleus.api.exceptions.ModuleNotLoadedException;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.data.value.mutable.MutableBoundedValue;
import org.spongepowered.api.data.value.mutable.OptionalValue;
import org.spongepowered.api.data.value.mutable.Value;

/**
 * Sponge Data API keys used in Nucleus.
 */
public final class NucleusKeys {

    private NucleusKeys() {}

    // Warp Keys

    /**
     * The name of the warp the sign represents.
     */
    public final static Key<Value<String>> WARP_NAME = new FakeKey<>();

    /**
     * The permission for the warp.
     */
    public final static Key<OptionalValue<String>> WARP_PERMISSION = new FakeKey<>();

    /**
     * The warmup for the warp.
     */
    public final static Key<MutableBoundedValue<String>> WARP_WARMUP = new FakeKey<>();

    /* (non-Javadoc)
     *
     * A fake key.
     */
    private static class FakeKey<E, T extends BaseValue<E>> implements Key<T> {

        @Override
        public Class<T> getValueClass() {
            throw new ModuleNotLoadedException();
        }

        @Override
        public DataQuery getQuery() {
            throw new ModuleNotLoadedException();
        }
    }
}
