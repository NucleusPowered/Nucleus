/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.module.staffchat;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageReceiver;

import java.util.Collection;
import java.util.UUID;

/**
 * Provides a way to get the Staff Chat message channel instance.
 */
public interface NucleusStaffChatService {

    /**
     * Gets if the provided {@link MessageChannelEvent.Chat} is going to be
     * sent to the staff chat channel.
     *
     * @param event The {@link MessageChannelEvent.Chat}
     * @return true if so
     */
    boolean isDirectedToStaffChat(MessageChannelEvent.Chat event);

    /**
     * Gets if the given {@link CommandSource} is currently talking
     * in staff chat.
     *
     * @param source The {@link CommandSource}
     * @return true if so
     */
    boolean isCurrentlyChattingInStaffChat(CommandSource source);

    /**
     * Gets if a player with the given {@link UUID} is currently talking
     * in staff chat.
     *
     * @param uuid The {@link UUID} of the {@link User}
     * @return true if so
     */
    boolean isCurrentlyChattingInStaffChat(UUID uuid);

    /**
     * Gets the memebers of the Staff Chat channel.
     *
     * @return The {@link CommandSource}s who are members of the channel.
     */
    Collection<MessageReceiver> getStaffChannelMembers();

    /**
     * Sends a message to the Staff Chat channel.
     *
     * @param source The {@link CommandSource} that is sending this message.
     * @param message The message to send.
     */
    void sendMessageFrom(CommandSource source, Text message);

}
