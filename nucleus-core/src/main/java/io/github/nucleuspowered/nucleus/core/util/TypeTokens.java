/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.util;

import io.github.nucleuspowered.nucleus.api.module.home.data.Home;
import io.github.nucleuspowered.nucleus.api.module.jail.data.Jail;
import io.github.nucleuspowered.nucleus.api.module.mail.data.MailMessage;
import io.github.nucleuspowered.nucleus.api.util.data.NamedLocation;
import io.github.nucleuspowered.nucleus.core.configurate.datatypes.LocationNode;
import io.leangen.geantyref.TypeToken;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.util.MinecraftDayTime;
import org.spongepowered.api.world.WorldType;
import org.spongepowered.api.world.difficulty.Difficulty;
import org.spongepowered.math.vector.Vector3d;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class TypeTokens {

    public static final TypeToken<Boolean> BOOLEAN = TypeToken.get(Boolean.class);

    public static final TypeToken<Instant> INSTANT = TypeToken.get(Instant.class);

    public static final TypeToken<Integer> INTEGER = TypeToken.get(Integer.class);

    public static final TypeToken<Locale> LOCALE = TypeToken.get(Locale.class);

    public static final TypeToken<LocationNode> LOCATION_NODE = TypeToken.get(LocationNode.class);

    public static final TypeToken<MinecraftDayTime> MINECRAFT_DAY_TIME = TypeToken.get(MinecraftDayTime.class);

    public static final TypeToken<NamedLocation> NAMED_LOCATION = TypeToken.get(NamedLocation.class);

    public static final TypeToken<Map<String, Home>> HOMES_MAP = new TypeToken<Map<String, Home>>() { };

    public static final TypeToken<ResourceKey> RESOURCE_KEY = TypeToken.get(ResourceKey.class);

    public static final TypeToken<String> STRING = TypeToken.get(String.class);

    public static final TypeToken<UUID> UUID = TypeToken.get(UUID.class);

    public static final TypeToken<Vector3d> VECTOR_3D = TypeToken.get(Vector3d.class);

    public static final TypeToken<MailMessage> MAIL_MESSAGE = TypeToken.get(MailMessage.class);

    public static final TypeToken<GameMode> GAME_MODE = TypeToken.get(GameMode.class);

    public static final TypeToken<Difficulty> DIFFICULTY = TypeToken.get(Difficulty.class);

    public static final TypeToken<WorldType> WORLD_TYPE = TypeToken.get(WorldType.class);

    public static final TypeToken<Jail> JAIL = TypeToken.get(Jail.class);

    private TypeTokens() {
        // no-op
    }

}
