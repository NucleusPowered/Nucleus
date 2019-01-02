/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal;

import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.configurate.datatypes.LocationNode;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TypeTokens {

    public static final TypeToken<Boolean> BOOLEAN = TypeToken.of(boolean.class);

    public static final TypeToken<Instant> INSTANT = TypeToken.of(Instant.class);

    public static final TypeToken<LocationNode> LOCATION_NODE = TypeToken.of(LocationNode.class);

    public static final TypeToken<Integer> INTEGER = TypeToken.of(int.class);

    public static final TypeToken<Long> LONG = TypeToken.of(long.class);

    public static final TypeToken<String> STRING = TypeToken.of(String.class);

    public static final TypeToken<Map<String, LocationNode>> LOCATIONS = new TypeToken<Map<String, LocationNode>>() {};

    public static final TypeToken<List<UUID>> UUID_LIST = new TypeToken<List<UUID>>() {};
}
