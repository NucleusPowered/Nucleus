/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.listeners;

import com.google.common.collect.ImmutableList;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IChatMessageFormatterService;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.channel.MutableMessageChannel;

import java.util.Optional;

public class ChatChannelListener implements ListenerBase {

    private final IChatMessageFormatterService chatMessageFormatter;

    public ChatChannelListener(INucleusServiceCollection serviceCollection) {
        this.chatMessageFormatter = serviceCollection.chatMessageFormatter();
    }

    // Used to perform any transformations so that they can be caught by other plugins.
    @Listener(order = Order.LATE)
    public void onChatMessageLast(MessageChannelEvent.Chat chat, @Root CommandSource source) {
        Optional<IChatMessageFormatterService.Channel> channelOptional =
                this.chatMessageFormatter.getNucleusChannel(Util.getUUID(source));
        if (channelOptional.map(IChatMessageFormatterService.Channel::willFormat).orElse(false)) {
            IChatMessageFormatterService.Channel channel = channelOptional.get();
            channel.formatMessageEvent(source, chat.getFormatter());
            chat.setChannel(chat.getChannel().map(x -> {
                MutableMessageChannel messageChannel = x.asMutable();
                // Copy to make sure we don't CME
                for (MessageReceiver toSendTo : ImmutableList.copyOf(messageChannel.getMembers())) {
                    if (!channel.receivers().contains(toSendTo)) {
                        // If the receiver is not in the channel, remove
                        messageChannel.removeMember(toSendTo);
                    }
                }

                return (MessageChannel) messageChannel;
            }).orElseGet(() -> MessageChannel.fixed(channel.receivers())));
        }
    }

}
