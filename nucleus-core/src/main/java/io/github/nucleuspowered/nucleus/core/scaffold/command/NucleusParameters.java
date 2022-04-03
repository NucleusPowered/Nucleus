/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.scaffold.command;

import io.github.nucleuspowered.nucleus.core.scaffold.command.parameter.AudienceValueParameter;
import io.github.nucleuspowered.nucleus.core.scaffold.command.parameter.OfflineWorldParameter;
import io.leangen.geantyref.TypeToken;
import io.vavr.control.Either;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.SystemSubject;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.CommonParameters;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.ValueParameterModifier;
import org.spongepowered.api.command.parameter.managed.standard.ResourceKeyedValueParameters;
import org.spongepowered.api.command.parameter.managed.standard.VariableValueParameters;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A selection of common parameters for making things consistent
 */
public final class NucleusParameters {

    public final static class Keys {

        private Keys() {
        }


        public static final String BOOL = "true|false";
        public static final String COMMAND = "command";
        public static final String CONSOLE = "-";
        public static final String DESCRIPTION = "description";
        public static final String DISPLAY_NAME = "display name";
        public static final String DURATION = "duration";
        public static final String GAME_PROFILE = "game profile";
        public static final String LOCATION = "location";
        public static final String LORE = "lore";
        public static final String MESSAGE = "message";
        public static final String NAME = "name";
        public static final String PLAYER = "player";
        public static final String REASON = "reason";
        public static final String SUBJECT = "subject";
        public static final String USER = "user";
        public static final String WORLD = "world";
    }

    private NucleusParameters() {
    } // entirely static

    public static final Parameter.Value<Boolean> ONE_TRUE_FALSE = Parameter.bool().key(Keys.BOOL).build();

    public static final Parameter.Value<Boolean> OPTIONAL_ONE_TRUE_FALSE = Parameter.bool().key(Keys.BOOL).optional().build();

    public static final Parameter.Value<List<Entity>> MANY_ENTITY = Parameter.builder(new TypeToken<List<Entity>>() {})
            .key(Keys.SUBJECT)
            .addParser(ResourceKeyedValueParameters.MANY_ENTITIES)
            .build();

    public static final Parameter.Value<List<ServerPlayer>> MANY_PLAYER = Parameter.builder(new TypeToken<List<ServerPlayer>>() {})
            .key(Keys.PLAYER)
            .addParser(ResourceKeyedValueParameters.MANY_PLAYERS)
            .build();

    public static final Parameter.Value<ServerPlayer> ONE_PLAYER = CommonParameters.PLAYER;

    public static final Parameter.Value<ServerPlayer> OPTIONAL_ONE_PLAYER = CommonParameters.PLAYER_OPTIONAL;

    public static final Parameter.Value<SystemSubject> CONSOLE_FROM_DASH = Parameter
            .builder(SystemSubject.class)
            .key(Keys.CONSOLE)
            .addParser(VariableValueParameters.literalBuilder(SystemSubject.class)
                    .literal(Collections.singletonList("-"))
                    .returnValue(Sponge::systemSubject)
                    .build())
            .build();

    public static final Parameter.Value<UUID> ONE_USER = Parameter.user().key(Keys.USER).build();

    public static final Parameter.Value<GameProfile> GAME_PROFILE =
            Parameter.builder(TypeToken.get(GameProfile.class)).key(Keys.GAME_PROFILE).addParser(ResourceKeyedValueParameters.GAME_PROFILE).build();

    public static final Parameter.Value<Component> DISPLAY_NAME_COMPONENT = Parameter.formattingCodeTextOfRemainingElements().key(Keys.DISPLAY_NAME).build();

    public static final Parameter.Value<String> COMMAND = Parameter.remainingJoinedStrings().key(Keys.COMMAND).build();

    public static final Parameter.Value<String> OPTIONAL_COMMAND = Parameter.remainingJoinedStrings().key(Keys.COMMAND).optional().build();

    public static final Parameter.Value<String> DESCRIPTION = Parameter.remainingJoinedStrings().key(Keys.DESCRIPTION).build();

    public static final Parameter.Value<Component> OPTIONAL_DESCRIPTION_COMPONENT =
            Parameter.formattingCodeTextOfRemainingElements().key(Keys.DESCRIPTION).optional().build();

    public static final Parameter.Value<Component> LORE = Parameter.formattingCodeTextOfRemainingElements().key(Keys.LORE).build();

    public static final Parameter.Value<String> MESSAGE = Parameter.remainingJoinedStrings().key(Keys.MESSAGE).build();

    public static final Parameter.Value<String> OPTIONAL_MESSAGE = Parameter.remainingJoinedStrings().key(Keys.MESSAGE).optional().build();

    public static final Parameter.Value<String> REASON = Parameter.remainingJoinedStrings().key(Keys.REASON).build();

    public static final Parameter.Value<String> OPTIONAL_REASON = Parameter.remainingJoinedStrings().key(Keys.REASON).optional().build();

    public static final Parameter.Value<ServerWorld> ONLINE_WORLD = CommonParameters.WORLD;

    public static final Parameter.Value<ServerWorld> ONLINE_WORLD_OPTIONAL = Parameter.world().optional().key(Keys.WORLD).build();

    public static final Parameter.Value<ResourceKey> OFFLINE_WORLD =
            Parameter.builder(ResourceKey.class)
                    .key(Keys.WORLD)
                    .addParser(new OfflineWorldParameter(
                            key -> Component.text(key.formatted() + " is not an unloaded world.")))
                    .build();

    public static final Parameter.Value<Duration> DURATION = Parameter.duration().key(Keys.DURATION).build();

    public static final Parameter.Value<Duration> OPTIONAL_DURATION = Parameter.duration().key(Keys.DURATION).optional().build();

    public static final Parameter.Value<ServerLocation> LOCATION = Parameter.location().key(Keys.LOCATION).build();

    public static final Parameter.Value<ServerLocation> OPTIONAL_LOCATION = Parameter.location().key(Keys.LOCATION).optional().build();

    public static final Parameter.Value<List<Audience>> MULTI_AUDIENCE = Parameter.builder(new TypeToken<List<Audience>>() {})
            .key("audience")
            .addParser(new AudienceValueParameter())
            .build();

    public static final Parameter.Value<String> STRING_NAME = Parameter.string().key(Keys.NAME).build();

    public static final Parameter.Value<Component> OPTIONAL_REASON_COMPONENT = Parameter.formattingCodeText().key("reason").optional().build();

    public static final Parameter.Value<Double> COST =
            Parameter.builder(Double.class).addParser(VariableValueParameters.doubleRange().min(0.0).build()).key("cost").build();

    public static final class Composite {

        public static final Parameter USER_OR_GAME_PROFILE = Parameter.firstOf(
                NucleusParameters.ONE_USER,
                NucleusParameters.GAME_PROFILE
        );
        public static final Parameter PLAYER_OR_CONSOLE = Parameter.firstOf(
                NucleusParameters.CONSOLE_FROM_DASH,
                NucleusParameters.ONE_PLAYER
        );

        public static Either<UUID, GameProfile> parseUserOrGameProfile(final ICommandContext context) {
            return context.getOne(NucleusParameters.ONE_USER)
                    .<Either<UUID, GameProfile>>map(Either::left)
                    .orElseGet(() -> Either.right(context.requireOne(NucleusParameters.GAME_PROFILE)));
        }

        public static Either<SystemSubject, ServerPlayer> parsePlayerOrConsole(final ICommandContext context) {
            return context.getOne(NucleusParameters.CONSOLE_FROM_DASH)
                    .<Either<SystemSubject, ServerPlayer>>map(Either::left)
                    .orElseGet(() -> Either.right(context.requireOne(NucleusParameters.ONE_PLAYER)));
        }
    }

}
