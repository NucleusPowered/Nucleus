/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.scaffold.command;

import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.ICommandModifier;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.world.server.ServerWorld;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;

public interface ICommandContext {

    ServerPlayer requirePlayer() throws CommandException;

    CommandCause cause();

    String getCommandKey();

    Object getCommandSourceRoot();

    Optional<UUID> uniqueId();

    /**
     * Gets the current cooldown for the command
     *
     * @return
     */
    int getCooldown();

    void setCooldown(int cooldown);

    double getCost();

    void setCost(double cost);

    int getWarmup();

    /**
     * Sets the warmup in seconds.
     *
     * @param warmup The warmup in seconds
     */
    void setWarmup(int warmup);

    ServerPlayer getPlayerFromArgs(Parameter.Key<? extends ServerPlayer> serverPlayer, String errorKey) throws CommandException;

    default ServerPlayer getPlayerFromArgs() throws CommandException {
        return this.getPlayerFromArgs(NucleusParameters.ONE_PLAYER.key(), "command.playeronly");
    }

    ServerPlayer getCommandSourceAsPlayerUnchecked();

    Optional<User> getOptionalUserFromUUID(final Parameter.Key<? extends UUID> key);

    Optional<User> getOptionalUserFromUUID(final Parameter.Value<? extends UUID> key);

    default User getUserFromArgs() throws CommandException {
        return this.getUserFromArgs(NucleusParameters.ONE_USER, "command.playeronly");
    }

    default User getUserFromArgs(final Parameter.Key<? extends UUID> key) throws CommandException {
        return this.getUserFromArgs(key, "command.playeronly");
    }

    default User getUserFromArgs(final Parameter.Value<? extends UUID> key) throws CommandException {
        return this.getUserFromArgs(key, "command.playeronly");
    }

    User getUserFromArgs(Parameter.Value<? extends UUID> key, String errorKey) throws CommandException;

    User getUserFromArgs(Parameter.Key<? extends UUID> key, String errorKey) throws CommandException;

    boolean hasFlag(String name);

    boolean hasAny(Parameter.Key<?> name);

    default boolean hasAny(final Parameter.Value<?> name) {
        return this.hasAny(name.key());
    }

    <T> Optional<T> getOne(Parameter.Key<T> name);

    <T> Optional<T> getOne(Parameter.Value<T> name);

    <T> Collection<? extends T> getAll(Parameter.Key<T> name);

    <T> Collection<? extends T> getAll(Parameter.Value<T> name);

    <T> T requireOne(Parameter.Key<T> key);

    <T> T requireOne(Parameter.Value<T> key);

    INucleusServiceCollection getServiceCollection();

    ICommandResult successResult();

    ICommandResult failResult();

    ICommandResult errorResultLiteral(Component message);

    ICommandResult errorResult(String key, Object... args);

    ICommandResult getResultFromBoolean(boolean success);

    CommandException createException(Throwable ex, String key, Object... args);

    CommandException createException(String key, Object... args);

    Optional<ServerPlayer> getAsPlayer();

    @NonNull
    default ServerPlayer getIfPlayer() throws CommandException {
        return this.getIfPlayer("command.playeronly");
    }

    @NonNull
    ServerPlayer getIfPlayer(String errorKey) throws CommandException;

    Map<CommandModifier, ICommandModifier> modifiers();

    Collection<Consumer<ICommandContext>> failActions();

    boolean testPermission(String permission);

    boolean testPermissionFor(UUID uuid, String permission);

    boolean testPermissionFor(Subject subject, String permission);

    String getMessageString(String key, Object... replacements);

    String getMessageStringFor(Audience to, String key, Object... replacements);

    Component getMessage(String key, Object... replacements);

    Component getMessageFor(Audience to, String key, Object... replacements);

    default String getTimeString(final Duration duration) {
        return this.getTimeString(duration.getSeconds());
    }

    String getTimeString(long seconds);

    /**
     * Sends a message to the command invoker.
     *
     * @param key The translation key
     * @param replacements The replacements
     */
    void sendMessage(String key, Object... replacements);

    void sendMessageText(Component message);

    void sendMessageTo(Audience to, String key, Object... replacements);

    boolean is(Object other);

    boolean is(Class<?> other);

    boolean is(User x);

    boolean is(UUID x);

    default boolean isNot(final User x) {
        return !this.is(x);
    }

    default boolean isNot(final UUID x) {
        return !this.is(x);
    }

    boolean isUser();

    boolean isConsoleAndBypass();

    default ServerWorld getWorldPropertiesOrFromSelf(final Parameter.Value<ServerWorld> worldKey) throws CommandException {
        return this.getWorldPropertiesOrFromSelf(worldKey.key());
    }

    ServerWorld getWorldPropertiesOrFromSelf(Parameter.Key<ServerWorld> worldKey) throws CommandException;

    default Optional<ServerWorld> getWorldPropertiesOrFromSelfOptional(final Parameter.Value<ServerWorld> worldKey) {
        return this.getWorldPropertiesOrFromSelfOptional(worldKey.key());
    }

    Optional<ServerWorld> getWorldPropertiesOrFromSelfOptional(Parameter.Key<ServerWorld> worldKey);

    String getName();

    Component getDisplayName();

    Component getDisplayName(UUID uuid);

    default String getTimeToNowString(final Instant endTime) {
        return this.getTimeString(Duration.between(Instant.now(), endTime).abs());
    }

    default OptionalInt getLevel(final String key) {
        return this.getLevelFor(this.cause(), key);
    }

    OptionalInt getLevelFor(Subject subject, String key);

    default int getLevel(final String key, final String permissionIfNoLevel) {
        return this.getLevelFor(this.cause(), key, permissionIfNoLevel);
    }

    default int getLevelFor(final Subject subject, final String key, final String permissionIfNoLevel) {
        return this.getLevelFor(subject, key).orElseGet(() -> this.testPermissionFor(subject, permissionIfNoLevel) ? 1 : 0);
    }

    /**
     * Gets whether the permission level is okay.
     *
     * @param actee The person that is targetted
     * @param key The level key
     * @param permissionIfNoLevel The permission to check if no level is provided
     * @param isSameLevel If true, this returns true if the actor and actee have the same permission level, if false,
     *                      returns false in the same situation.
     * @return if the level is okay to proceed
     */
    boolean isPermissionLevelOkay(Subject actee, String key, String permissionIfNoLevel, boolean isSameLevel);

    boolean isPermissionLevelOkay(UUID actee, String key, String permissionIfNoLevel, boolean isSameLevel);

    void removeModifier(String modifierId);

    void removeModifier(ICommandModifier modifier);

    void addFailAction(Consumer<ICommandContext> action);

    Audience audience();

    Locale getLocale();
}
