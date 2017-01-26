/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.spongedata;

import com.google.common.reflect.TypeToken;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.value.mutable.OptionalValue;

import java.util.Optional;

public class NucleusSpongeDataKeys {

    private NucleusSpongeDataKeys() {}

    public static final Key<OptionalValue<String>> NUCLEUS_PERMISSION = KeyFactory.makeOptionalKey(
        new TypeToken<Optional<String>>() {},
        new TypeToken<OptionalValue<String>>() {},
        DataQuery.of("nucleus", "permission"),
        "nucleus:permission",
        "Required Permission"
    );

    public static final Key<OptionalValue<Double>> NUCLEUS_COST = KeyFactory.makeOptionalKey(
        new TypeToken<Optional<Double>>() {},
        new TypeToken<OptionalValue<Double>>() {},
        DataQuery.of("nucleus", "cost"),
        "nucleus:cost",
        "Cost"
    );
}
