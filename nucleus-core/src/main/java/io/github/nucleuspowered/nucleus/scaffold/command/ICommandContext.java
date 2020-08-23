/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.scaffold.command;

import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.ICommandModifier;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.world.storage.WorldProperties;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;

public interface ICommandContext<C extends CommandSource> {

    Cause getCause();

    String getCommandKey();

    C getCommandSource() throws CommandException;

    C getCommandSourceUnchecked();

    Optional<UUID> getUniqueId();

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

    Player getPlayerFromArgs(String key, String errorKey) throws CommandException;

    default Player getPlayerFromArgs() throws CommandException {
        return getPlayerFromArgs(NucleusParameters.Keys.PLAYER, "command.playeronly");
    }

    Player getCommandSourceAsPlayerUnchecked();

    default User getUserFromArgs() throws CommandException {
        return getUserFromArgs(NucleusParameters.Keys.USER, "command.playeronly");
    }

    User getUserFromArgs(String key, String errorKey) throws CommandException;

    boolean hasAny(String name);

    <T> Optional<T> getOne(String name, Class<T> clazz);

    <T> Optional<T> getOne(String name, TypeToken<T> clazz);

    <T> Collection<T> getAll(String name, Class<T> clazz);

    <T> Collection<T> getAll(String name, TypeToken<T> clazz);

    @NonNull
    <T> T requireOne(String name, Class<T> clazz);

    @NonNull
    <T> T requireOne(String name, TypeToken<T> clazz);

    INucleusServiceCollection getServiceCollection();

    ICommandResult successResult();

    ICommandResult failResult();

    ICommandResult errorResultLiteral(Text message);

    ICommandResult errorResult(String key, Object... args);

    ICommandResult getResultFromBoolean(boolean success);

    CommandException createException(Throwable ex, String key, Object... args);

    CommandException createException(String key, Object... args);

    @NonNull
    default Player getIfPlayer() throws CommandException {
        return getIfPlayer("command.playeronly");
    }

    @NonNull
    Player getIfPlayer(String errorKey) throws CommandException;

    Map<CommandModifier, ICommandModifier> modifiers();

    Collection<Consumer<ICommandContext<C>>> failActions();

    boolean testPermission(String permission);

    boolean testPermissionFor(Subject subject, String permission);

    String getMessageString(String key, Object... replacements);

    String getMessageStringFor(CommandSource to, String key, Object... replacements);

    Text getMessage(String key, Object... replacements);

    Text getMessageFor(CommandSource to, String key, Object... replacements);

    default String getTimeString(Duration duration) {
        return getTimeString(duration.getSeconds());
    }

    String getTimeString(long seconds);

    /**
     * Sends a message to the command invoker.
     *
     * @param key The translation key
     * @param replacements The replacements
     */
    void sendMessage(String key, Object... replacements);

    void sendMessageText(Text message);

    void sendMessageTo(MessageReceiver to, String key, Object... replacements);

    default boolean is(Player other) {
        return is(other.getCommandSource().get());
    }

    boolean is(CommandSource other);

    boolean is(Class<?> other);

    boolean is(User x);

    default boolean isNot(User x) {
        return !this.is(x);
    }

    boolean isUser();

    boolean isConsoleAndBypass();

    Optional<WorldProperties> getWorldPropertiesOrFromSelf(String worldKey);

    String getName();

    Text getDisplayName();

    Text getDisplayName(UUID uuid);

    default String getTimeToNowString(Instant endTime) {
        return getTimeString(Duration.between(Instant.now(), endTime).abs());
    }

    default OptionalInt getLevel(String key) {
        return getLevelFor(getCommandSourceUnchecked(), key);
    }

    OptionalInt getLevelFor(Subject subject, String key);

    default int getLevel(String key, String permissionIfNoLevel) {
        return getLevelFor(getCommandSourceUnchecked(), key, permissionIfNoLevel);
    }

    default int getLevelFor(Subject subject, String key, String permissionIfNoLevel) {
        return getLevelFor(subject, key).orElseGet(() -> testPermissionFor(subject, permissionIfNoLevel) ? 1 : 0);
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

    interface Mutable<C extends CommandSource> extends ICommandContext<C> {

        <T> void put(String name, Class<T> clazz, T obj);

        <T> void putAll(String name, Class<T> clazz, Collection<? extends T> obj);

        void removeModifier(String modifierId);

        void removeModifier(ICommandModifier modifier);

        void addFailAction(Consumer<ICommandContext<C>> action);

    }

}
