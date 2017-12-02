/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.service;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Identifiable;
import org.spongepowered.api.util.Tristate;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nullable;

/**
 * A service that contains message related APIs.
 */
public interface NucleusPrivateMessagingService {

    /**
     * Returns whether the user is able to see all private messages sent on the server. This indicates that the user
     * has the correct permission AND has activated it.
     *
     * @param user The {@link User} to check.
     * @return <code>true</code> if the user has Social Spy enabled.
     */
    boolean isSocialSpy(User user);

    /**
     * Returns whether the server is using social spy levels.
     *
     * @return <code>true</code> if so.
     */
    boolean isUsingSocialSpyLevels();

    /**
     * If using social spy levels, this returns whether those on the same social spy level as the participants of a message can read a message.
     *
     * <p>
     *     In the following scenarios, Alice has a social spy level of 5, and Bob a social spy level of 10.
     * </p>
     * <p>
     *     If this method returns <code>true</code>, and Alice sends a message to Bob, Eve must have a level of 10 or above, to see the message. If
     *     Eve has a level of 9 or below, then because Bob's level is higher than hers, she <em>cannot</em> see the message.
     * </p>
     * <p>
     *     If this method returns <code>false</code>, and Alice sends a message to Bob, Eve must have a level of <strong>11</strong> or above, to see
     *     the message. If Eve has a level of 10 or below, then because Bob's level is <strong>the same</strong> than hers, she <em>cannot</em> see
     *     the message.
     * </p>
     *
     * @return Whether the server allows those with the same social spy level to spy on each other.
     */
    boolean canSpySameLevel();

    /**
     * Gets the social spy level for any message targets registered using {@link #registerMessageTarget(UUID, String, Text, Supplier)}.
     *
     * @return The target's social spy level.
     */
    int getCustomTargetLevel();

    /**
     * Gets the social spy level the server is assigned. Typically (but not always) this is {@link Integer#MAX_VALUE}, or zero if levels aren't
     * enabled.
     *
     * @return The console/server's social spy level.
     */
    int getServerLevel();

    /**
     * Gets the social spy level the user is assigned. This is zero by default.
     *
     * @param user The user to check.
     * @return The level.
     */
    int getSocialSpyLevel(User user);

    /**
     * Returns whether the specified user can toggle social spy {@link Tristate#UNDEFINED}, or whether they are forced to use social spy {@link
     * Tristate#TRUE}, or do not have permission to do so {@link Tristate#FALSE}.
     *
     * @param user The use to check.
     * @return A {@link Tristate} that indicates what state the user might be forced into.
     */
    Tristate forcedSocialSpyState(User user);

    /**
     * Sets whether the user is able to see all private messages on the server. This method will return whether the
     * system has fulfilled the request.
     *
     * @param user The {@link User}
     * @param isSocialSpy <code>true</code> to turn Social Spy on, <code>false</code> otherwise.
     * @return <code>true</code> if the change was fulfilled, <code>false</code> if the user does not have permission
     */
    boolean setSocialSpy(User user, boolean isSocialSpy);

    /**
     * Returns whether the specified user can spy on <strong>all</strong> of the specified sources.
     *
     * <p>
     *     This will return <code>false</code> if the spying user is also in the list of users to spy on.
     * </p>
     *
     * @param spyingUser The user that will be spying.
     * @param sourcesToSpyOn The {@link CommandSource}s to spy upon.
     * @return <code>true</code> if the user can spy on <strong>all</strong> the users.
     * @throws IllegalArgumentException thrown if no {@link CommandSource}s are supplied.
     */
    boolean canSpyOn(User spyingUser, CommandSource... sourcesToSpyOn) throws IllegalArgumentException;

    /**
     * Returns the {@link CommandSource}s that are online and can spy on <strong>all</strong> of the specified sources.
     *
     * <p>
     *     This will not return players in the list of users to spy on.
     * </p>
     *
     * @param includeConsole Whether to include the console in the returned {@link Set}.
     * @param sourcesToSpyOn The {@link CommandSource}s to spy upon.
     * @return A {@link Set} of {@link CommandSource}s that can spy upon the specified users.
     * @throws IllegalArgumentException thrown if no {@link CommandSource}s are supplied.
     */
    Set<CommandSource> onlinePlayersCanSpyOn(boolean includeConsole, CommandSource... sourcesToSpyOn) throws IllegalArgumentException;

    /**
     * Sends a message as the sender to the receiver. Takes a string to mirror what the command would do.
     *
     * @param sender The sender.
     * @param receiver The reciever.
     * @param message The message to send.
     * @return <code>true</code> if the message was sent, <code>false</code> otherwise.
     */
    boolean sendMessage(CommandSource sender, CommandSource receiver, String message);

    /**
     * Gets the {@link CommandSource} that the console will reply to if <code>/r</code> is used, if any.
     *
     * @return The {@link CommandSource}.
     */
    Optional<CommandSource> getConsoleReplyTo();

    /**
     * Gets the {@link CommandSource} that the specified {@link User} will reply to if <code>/r</code> is used, if any.
     *
     * @param from The {@link User} to inspect.
     * @return The {@link CommandSource}.
     */
    Optional<CommandSource> getReplyTo(User from);

    /**
     * Gets the {@link CommandSource} that the specified {@link T} will reply to if <code>/r</code> is used, if any.
     *
     * @param from The {@link T} to inspect.
     * @param <T> The {@link Identifiable} {@link CommandSource} type.
     * @return The {@link CommandSource}.
     */
    <T extends CommandSource & Identifiable> Optional<CommandSource> getCommandSourceReplyTo(T from);

    /**
     * Sets the {@link CommandSource} that the specified {@link User} will reply to if <code>/r</code> is used.
     * @param user The {@link User} to modify.
     * @param toReplyTo The {@link CommandSource}.
     */
    void setReplyTo(User user, CommandSource toReplyTo);

    /**
     * Sets the {@link CommandSource} that the console will reply to if <code>/r</code> is used.
     * @param toReplyTo The {@link CommandSource}.
     */
    void setConsoleReplyTo(CommandSource toReplyTo);

    /**
     * Sets the {@link CommandSource} that the specified {@link CommandSource} will reply to if <code>/r</code> is used.
     *
     * <p>
     *     If the {@link CommandSource} in both cases isn't a normal target or isn't registered, this will fail silently.
     * </p>
     *
     * @param source The {@link T} to modify.
     * @param <T> The {@link Identifiable} {@link CommandSource} type.
     * @param replyTo The {@link CommandSource} to now reply to.
     */
    <T extends CommandSource & Identifiable> void setCommandSourceReplyTo(T source, CommandSource replyTo);

    /**
     * Removes the {@link CommandSource} that the specified {@link User} will reply to if <code>/r</code> is used.
     * @param user The {@link User} to modify.
     */
    void clearReplyTo(User user);

    /**
     * Removes the {@link CommandSource} that the specified {@link CommandSource} will reply to if <code>/r</code> is used.
     * @param user The {@link User} to modify.
     * @param <T> The {@link Identifiable} {@link CommandSource} type.
     */
    <T extends CommandSource & Identifiable> void clearCommandSourceReplyTo(T user);

    /**
     * Removes the {@link CommandSource} that the console will reply to if <code>/r</code> is used.
     */
    void clearConsoleReplyTo();

    /**
     * Registers a message target. The target's UUID must remain constant, otherwise Nucleus will treat any interaction as coming from the console.
     *
     * <p>
     *     A message target is, perhaps unsurprisingly, a valid target for messaging. Players can message this target using
     *     <code>/m &lt;targetName&gt; [message]</code>
     * </p>
     * <p>
     *     Message targets <em>cannot</em> be ignored. They are mostly intended for bot use.
     * </p>
     *
     * @param uniqueId The {@link UUID} of the target.
     * @param targetName The name of the target in commands.
     * @param displayName The display name of the target. If <code>null</code>, {@link CommandSource#getName()} is used.
     * @param target A {@link Supplier} of the message target, which must implement both {@link CommandSource} and {@link Identifiable}.
     * @param <T> The type that implements both {@link CommandSource} and {@link Identifiable}.
     * @throws NullPointerException thrown if any non {@link Nullable} parameter is null.
     * @throws IllegalArgumentException thrown if the {@link UUID} is the zero UUID.
     * @throws IllegalStateException thrown if the {@link UUID} has already been registered.
     */
    <T extends CommandSource & Identifiable> void registerMessageTarget(UUID uniqueId, String targetName, @Nullable Text displayName, Supplier<T> target)
        throws NullPointerException, IllegalArgumentException, IllegalStateException;

    /**
     * Removes a previously registered message target.
     *
     * @param uniqueId The {@link UUID} of the target.
     * @throws IllegalStateException thrown if the {@link UUID} is not registered.
     */
    void removeMessageTarget(UUID uniqueId) throws IllegalStateException;
}
