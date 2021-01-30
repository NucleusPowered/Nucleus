/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.util;

import io.github.nucleuspowered.nucleus.core.configurate.datatypes.LocationNode;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.math.vector.Vector3d;
import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.api.module.mail.data.MailMessage;
import io.github.nucleuspowered.nucleus.api.module.warp.data.Warp;
import io.github.nucleuspowered.nucleus.api.module.warp.data.WarpCategory;
import io.github.nucleuspowered.nucleus.api.util.data.NamedLocation;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.world.storage.WorldProperties;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

@SuppressWarnings("UnstableApiUsage")
public final class TypeTokens {

    public static final TypeToken<Boolean> BOOLEAN = TypeToken.of(Boolean.class);

    public static final TypeToken<Instant> INSTANT = TypeToken.of(Instant.class);

    public static final TypeToken<ServerLocation> LOCATION_WORLD = TypeToken.of(ServerLocation.class);

    public static final TypeToken<LocationNode> LOCATION_NODE = TypeToken.of(LocationNode.class);

    public static final TypeToken<Integer> INTEGER = TypeToken.of(Integer.class);

    public static final TypeToken<Long> LONG = TypeToken.of(Long.class);

    public static final TypeToken<String> STRING = TypeToken.of(String.class);

    public static final TypeToken<Map<String, LocationNode>> LOCATIONS = new TypeToken<Map<String, LocationNode>>() {};

    public static final TypeToken<List<UUID>> UUID_LIST = new TypeToken<List<UUID>>() {};

    public static final TypeToken<Vector3d> VECTOR_3D = TypeToken.of(Vector3d.class);

    public static final TypeToken<Warp> WARP = TypeToken.of(Warp.class);

    public static final TypeToken<WarpCategory> WARP_CATEGORY = TypeToken.of(WarpCategory.class);

    public static final TypeToken<UUID> UUID = TypeToken.of(UUID.class);

    public static final TypeToken<NamedLocation> NAMEDLOCATION = TypeToken.of(NamedLocation.class);

    public static final TypeToken<MailMessage> MAIL_MESSAGE = TypeToken.of(MailMessage.class);

    public static final TypeToken<Entity> ENTITY = TypeToken.of(Entity.class);

    public static final TypeToken<WorldProperties> WORLD_PROPERTIES = TypeToken.of(WorldProperties.class);

    public static final TypeToken<Predicate<Entity>> PREDICATE_ENTITY = new TypeToken<Predicate<Entity>>() {};

    public static final TypeToken<Locale> LOCALE = TypeToken.of(Locale.class);

    public static final TypeToken<ResourceKey> RESOURCE_KEY = TypeToken.of(ResourceKey.class);
}