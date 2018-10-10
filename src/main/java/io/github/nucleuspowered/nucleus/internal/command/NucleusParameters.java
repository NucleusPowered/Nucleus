/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.command;

import static io.github.nucleuspowered.nucleus.internal.command.NucleusParameters.Keys.WORLD;
import static io.github.nucleuspowered.nucleus.internal.command.NucleusParameters.Keys.XYZ;

import io.github.nucleuspowered.nucleus.argumentparsers.GameProfileArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.NicknameArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.NucleusWorldPropertiesArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.SelectorArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.TimespanArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.UUIDArgument;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

/**
 * A selection of common parameters for making things consistent
 */
public class NucleusParameters {

    public static class Keys {

        private Keys() {}

        public static final String BOOL = "true|false";
        public static final String COMMAND = "command";
        public static final String DESCRIPTION = "description";
        public static final String DURATION = "duration";
        public static final String LOCATION = "location";
        public static final String LORE = "lore";
        public static final String MESSAGE = "message";
        public static final String PLAYER = "player";
        public static final String PLAYER_OR_CONSOLE = "player|-";
        public static final String REASON = "reason";
        public static final String SUBJECT = "subject";
        public static final String USER = "user";
        public static final String USER_UUID = "user uuid";
        public static final String WORLD = "world";
        public static final String XYZ = "x y z";
    }

    private NucleusParameters() {} // entirely static

    public static final CommandElement ONE_TRUE_FALSE = GenericArguments.onlyOne(GenericArguments.bool(Text.of(Keys.BOOL)));

    public static final CommandElement OPTIONAL_ONE_TRUE_FALSE = GenericArguments.optional(ONE_TRUE_FALSE);

    public static final CommandElement MANY_ENTITY =
            new SelectorArgument(new NicknameArgument(Text.of(Keys.SUBJECT), NicknameArgument.Target.PLAYER), Entity.class);

    public static final CommandElement MANY_LIVING =
            new SelectorArgument(new NicknameArgument(Text.of(Keys.SUBJECT), NicknameArgument.Target.PLAYER), Living.class);

    // users
    public static final CommandElement MANY_PLAYER_NO_SELECTOR = new NicknameArgument(Text.of(Keys.PLAYER), NicknameArgument.Target.PLAYER);

    public static final CommandElement MANY_USER_NO_SELECTOR = new NicknameArgument(Text.of(Keys.USER), NicknameArgument.Target.USER);

    public static final CommandElement ONE_PLAYER_NO_SELECTOR = GenericArguments.onlyOne(MANY_PLAYER_NO_SELECTOR);

    public static final CommandElement MANY_PLAYER = new SelectorArgument(MANY_PLAYER_NO_SELECTOR, Player.class);

    public static final CommandElement ONE_PLAYER = GenericArguments.onlyOne(MANY_PLAYER);

    public static final CommandElement OPTIONAL_ONE_PLAYER = GenericArguments.optionalWeak(ONE_PLAYER);

    public static final CommandElement MANY_PLAYER_OR_CONSOLE =
            new SelectorArgument(new NicknameArgument(Text.of(Keys.PLAYER_OR_CONSOLE), NicknameArgument.Target.PLAYER), Player.class);

    public static final CommandElement ONE_PLAYER_OR_CONSOLE = GenericArguments.onlyOne(MANY_PLAYER_OR_CONSOLE);

    public static final CommandElement ONE_USER =
            GenericArguments.onlyOne(new SelectorArgument(new NicknameArgument(Text.of(Keys.USER), NicknameArgument.Target.USER), Player.class));

    public static final CommandElement ONE_USER_PLAYER_KEY =
            GenericArguments.onlyOne(new SelectorArgument(new NicknameArgument(Text.of(Keys.PLAYER), NicknameArgument.Target.USER), Player.class));

    public static final CommandElement ONE_USER_UUID = GenericArguments.onlyOne(UUIDArgument.user(Text.of(Keys.USER_UUID)));

    public static final CommandElement ONE_GAME_PROFILE_UUID = GenericArguments.onlyOne(UUIDArgument.gameProfile(Text.of(Keys.USER_UUID)));

    public static final CommandElement ONE_GAME_PROFILE = GenericArguments.onlyOne(new GameProfileArgument(Text.of(Keys.USER)));

    public static final CommandElement COMMAND = GenericArguments.remainingRawJoinedStrings(Text.of(Keys.COMMAND));

    public static final CommandElement OPTIONAL_COMMAND = GenericArguments.optional(COMMAND);

    public static final CommandElement DESCRIPTION = GenericArguments.remainingRawJoinedStrings(Text.of(Keys.DESCRIPTION));

    public static final CommandElement OPTIONAL_DESCRIPTION = GenericArguments.optional(DESCRIPTION);

    public static final CommandElement LORE = GenericArguments.remainingRawJoinedStrings(Text.of(Keys.LORE));

    public static final CommandElement MESSAGE = GenericArguments.remainingRawJoinedStrings(Text.of(Keys.MESSAGE));

    public static final CommandElement OPTIONAL_MESSAGE = GenericArguments.optional(MESSAGE);

    public static final CommandElement REASON = GenericArguments.remainingRawJoinedStrings(Text.of(Keys.REASON));

    public static final CommandElement OPTIONAL_REASON = GenericArguments.optional(REASON);

    public static final CommandElement WORLD_PROPERTIES_ENABLED_ONLY = new NucleusWorldPropertiesArgument(Text.of(WORLD),
            NucleusWorldPropertiesArgument.Type.ENABLED_ONLY);

    public static final CommandElement OPTIONAL_WEAK_WORLD_PROPERTIES_ENABLED_ONLY =
            GenericArguments.optionalWeak(new NucleusWorldPropertiesArgument(Text.of(WORLD), NucleusWorldPropertiesArgument.Type.ENABLED_ONLY));

    public static final CommandElement OPTIONAL_WORLD_PROPERTIES_ENABLED_ONLY =
            GenericArguments.optional(new NucleusWorldPropertiesArgument(Text.of(WORLD), NucleusWorldPropertiesArgument.Type.ENABLED_ONLY));

    public static final CommandElement WORLD_PROPERTIES_DISABLED_ONLY = new NucleusWorldPropertiesArgument(Text.of(WORLD),
            NucleusWorldPropertiesArgument.Type.DISABLED_ONLY);

    public static final CommandElement WORLD_PROPERTIES_ALL = GenericArguments.onlyOne(
            new NucleusWorldPropertiesArgument(Text.of(WORLD), NucleusWorldPropertiesArgument.Type.ALL));

    public static final CommandElement OPTIONAL_WORLD_PROPERTIES_ALL = GenericArguments.optionalWeak(WORLD_PROPERTIES_ALL);

    public static final CommandElement WORLD_PROPERTIES_UNLOADED_ONLY = GenericArguments.onlyOne(
            new NucleusWorldPropertiesArgument(Text.of(WORLD), NucleusWorldPropertiesArgument.Type.UNLOADED_ONLY));

    public static final CommandElement WORLD_PROPERTIES_LOADED_ONLY = GenericArguments.onlyOne(
            new NucleusWorldPropertiesArgument(Text.of(WORLD), NucleusWorldPropertiesArgument.Type.LOADED_ONLY));

    public static final CommandElement DURATION = GenericArguments.onlyOne(new TimespanArgument(Text.of(Keys.DURATION)));

    public static final CommandElement OPTIONAL_DURATION = GenericArguments.optional(DURATION);

    public static final CommandElement OPTIONAL_WEAK_DURATION = GenericArguments.optionalWeak(DURATION);

    public static final CommandElement POSITION = GenericArguments.onlyOne(GenericArguments.vector3d(Text.of(XYZ)));

    public static final CommandElement LOCATION = GenericArguments.onlyOne(GenericArguments.location(Text.of(Keys.LOCATION)));

    public static final CommandElement OPTIONAL_LOCATION = GenericArguments.optional(LOCATION);
}
