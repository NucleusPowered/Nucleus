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
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.value.mutable.MutableBoundedValue;

import java.util.HashMap;
import java.util.Map;

public final class SpongeDataRegistrar {

    private static final Map<String, Key> keys = new HashMap<String, Key>() {{
        put("WARP_NAME", KeyFactory.makeOptionalKey(String.class, DataQuery.of("nucleus", "warp", "name")));
        put("WARP_PERMISSION", KeyFactory.makeOptionalKey(String.class, DataQuery.of("nucleus", "warp", "permission")));
        put("WARP_WARMUP", KeyFactory.makeSingleKey(Integer.class, MutableBoundedValue.class, DataQuery.of("nucleus", "warp", "warmup")));
        put("WARP_COST", KeyFactory.makeSingleKey(Integer.class, MutableBoundedValue.class, DataQuery.of("nucleus", "warp", "cost")));
    }};

    private SpongeDataRegistrar() {}

    public static void registerData() throws NoSuchFieldException, IllegalAccessException {
        DataManager dataManager = Sponge.getDataManager();

        for (Map.Entry<String, Key> entry : keys.entrySet()) {
            NucleusKeys.class.getDeclaredField(entry.getKey()).set(null, entry.getValue());
        }

        dataManager.register(WarpSignData.class, ImmutableWarpSignData.class, new NucleusWarpSignDataManipulatorBuilder());
    }
}
