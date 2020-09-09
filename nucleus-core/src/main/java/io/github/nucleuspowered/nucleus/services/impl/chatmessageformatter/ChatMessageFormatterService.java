/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.chatmessageformatter;

import com.google.common.base.Preconditions;
import io.github.nucleuspowered.nucleus.api.util.NoExceptionAutoClosable;
import io.github.nucleuspowered.nucleus.services.interfaces.IChatMessageFormatterService;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.MessageReceiver;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

import javax.inject.Singleton;

@Singleton
public class ChatMessageFormatterService implements IChatMessageFormatterService {

    private final Map<UUID, Channel> chatChannels = new HashMap<>();
    private final Map<CommandSource, Channel> sourceChannelMap = new WeakHashMap<>();

    @Override
    public Optional<Channel> getNucleusChannel(CommandSource source) {
        if (source instanceof User) {
            return this.getNucleusChannel(((User) source).getUniqueId());
        }
        return Optional.ofNullable(this.sourceChannelMap.get(source));
    }

    @Override
    public Optional<Channel> getNucleusChannel(UUID uuid) {
        return Optional.ofNullable(this.chatChannels.get(uuid));
    }

    @Override
    public void setPlayerNucleusChannel(UUID uuid, @Nullable Channel channel) {
        if (channel == null) {
            this.chatChannels.remove(uuid);
        } else {
            this.chatChannels.put(uuid, channel);
        }
    }

    @Override
    public NoExceptionAutoClosable setCommandSourceNucleusChannelTemporarily(final CommandSource source, final Channel channel) {
        if (source instanceof User) {
            return this.setPlayerNucleusChannelTemporarily(((User) source).getUniqueId(), channel);
        }
        Preconditions.checkNotNull(channel);
        this.sourceChannelMap.put(source, channel);
        final MessageChannel originalChannel = source.getMessageChannel();
        if (channel instanceof Channel.External<?>) {
            final MessageChannel newChannel = ((Channel.External<?>) channel).createChannel(originalChannel);
            source.setMessageChannel(newChannel);
        }
        return () -> {
            this.sourceChannelMap.remove(source);
            Sponge.getServer().getConsole().setMessageChannel(originalChannel);
        };
    }

    @Override
    public NoExceptionAutoClosable setPlayerNucleusChannelTemporarily(UUID uuid, Channel channel) {
        Preconditions.checkNotNull(channel);
        final Channel original = this.chatChannels.get(uuid);
        final Optional<Player> player = Sponge.getServer().getPlayer(uuid);
        final MessageChannel originalChannel = player.map(MessageReceiver::getMessageChannel).orElse(null);
        this.chatChannels.put(uuid, channel);
        if (channel instanceof Channel.External<?>) {
            final MessageChannel newChannel = ((Channel.External<?>) channel).createChannel(originalChannel);
            player.ifPresent(x -> x.setMessageChannel(newChannel));
        }
        return () -> {
            this.setPlayerNucleusChannel(uuid, original);
            if (originalChannel != null) {
                player.ifPresent(x -> x.setMessageChannel(originalChannel));
            } else {
                player.ifPresent(x -> x.setMessageChannel(MessageChannel.TO_ALL));
            }
        };
    }

}
