/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.module.staffchat;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageReceiver;

import java.util.Collection;

/**
 * Provides a way to get the Staff Chat message channel instance.
 */
public interface NucleusStaffChatService {

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
