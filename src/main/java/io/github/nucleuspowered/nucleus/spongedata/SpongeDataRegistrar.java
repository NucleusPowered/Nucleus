/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.spongedata;

import io.github.nucleuspowered.nucleus.api.spongedata.NucleusKeys;
import io.github.nucleuspowered.nucleus.api.spongedata.warp.ImmutableWarpSignData;
import io.github.nucleuspowered.nucleus.api.spongedata.warp.WarpSignData;
import io.github.nucleuspowered.nucleus.spongedata.warp.NucleusWarpSignDataManipulatorBuilder;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataManager;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.value.mutable.MutableBoundedValue;
import org.spongepowered.api.data.value.mutable.Value;

public final class SpongeDataRegistrar {

    private SpongeDataRegistrar() {}

    public static void registerData() throws NoSuchFieldException, IllegalAccessException {
        DataManager dataManager = Sponge.getDataManager();

        // Warp sign.
        NucleusKeys.class.getDeclaredField("WARP_NAME").set(null, KeyFactory.makeSingleKey(String.class, Value.class, DataQuery.of("nucleus", "warp", "name")));
        NucleusKeys.class.getDeclaredField("WARP_PERMISSION").set(null, KeyFactory.makeOptionalKey(String.class, DataQuery.of("nucleus", "warp", "permission")));
        NucleusKeys.class.getDeclaredField("WARP_WARMUP").set(null, KeyFactory.makeSingleKey(String.class, MutableBoundedValue.class, DataQuery.of("nucleus", "warp", "warmup")));
        NucleusKeys.class.getDeclaredField("WARP_COST").set(null, KeyFactory.makeOptionalKey(String.class, DataQuery.of("nucleus", "warp", "cost")));

        dataManager.register(WarpSignData.class, ImmutableWarpSignData.class, new NucleusWarpSignDataManipulatorBuilder());
    }
}
