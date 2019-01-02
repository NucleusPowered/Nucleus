/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ignore.listeners;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.chat.NucleusNoIgnoreChannel;
import io.github.nucleuspowered.nucleus.api.events.NucleusMailEvent;
import io.github.nucleuspowered.nucleus.api.events.NucleusMessageEvent;
import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import io.github.nucleuspowered.nucleus.internal.interfaces.ListenerBase;
import io.github.nucleuspowered.nucleus.modules.ignore.commands.IgnoreCommand;
import io.github.nucleuspowered.nucleus.modules.ignore.services.IgnoreService;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.channel.MutableMessageChannel;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class IgnoreListener implements ListenerBase {

    private final IgnoreService service = Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(IgnoreService.class);
    private CommandPermissionHandler ignoreHandler = Nucleus.getNucleus().getPermissionRegistry().getPermissionsForNucleusCommand(IgnoreCommand.class);

    @Listener(order = Order.LATE)
    public void onChat(MessageChannelEvent.Chat event) {
        if (event.getChannel().orElseGet(event::getOriginalChannel) instanceof NucleusNoIgnoreChannel) {
            return;
        }

        Util.onPlayerSimulatedOrPlayer(event, this::onChat);
    }

    private void onChat(MessageChannelEvent.Chat event, Player player) {
        // Reset the channel - but only if we have to.
        checkCancels(event.getChannel().orElseGet(event::getOriginalChannel).getMembers(), player).ifPresent(x -> {
            MutableMessageChannel mmc = event.getChannel().orElseGet(event::getOriginalChannel).asMutable();
            x.forEach(mmc::removeMember);
            event.setChannel(mmc);
        });
    }

    @Listener(order = Order.FIRST)
    public void onMessage(NucleusMessageEvent event, @Root Player player) {
        if (event.getRecipient() instanceof User) {
            try {
                event.setCancelled(this.service.isIgnored(((User) event.getRecipient()).getUniqueId(), player.getUniqueId()));
            } catch (Exception e) {
                if (Nucleus.getNucleus().isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Listener(order = Order.FIRST)
    public void onMail(NucleusMailEvent event, @Root Player player) {
        try {
            event.setCancelled(this.service.isIgnored(event.getRecipient().getUniqueId(), player.getUniqueId()));
        } catch (Exception e) {
            if (Nucleus.getNucleus().isDebugMode()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Checks if we need to cancel messages to people.
     *
     * @param collection The collection to check through.
     * @param player The subject who is sending the message.
     * @return {@link Optional} if unchanged, otherwise a {@link Collection} of {@link MessageReceiver}s to remove
     */
    private Optional<Collection<MessageReceiver>> checkCancels(Collection<MessageReceiver> collection, Player player) {
        if (this.ignoreHandler.testSuffix(player, "exempt.chat")) {
            return Optional.empty();
        }

        List<MessageReceiver> list = Lists.newArrayList(collection);
        list.removeIf(x -> {
            try {
                if (!(x instanceof Player)) {
                    // Remove if not a player.
                    return true;
                }

                if (x.equals(player)) {
                    // Remove if the same player.
                    return true;
                }

                // Don't remove if they are in the list.
                return !this.service.isIgnored(((Player) x).getUniqueId(), player.getUniqueId());
            } catch (Exception e) {
                if (Nucleus.getNucleus().isDebugMode()) {
                    e.printStackTrace();
                }

                // Remove them.
                return true;
            }
        });

        // We do this so we don't have to recreate a channel if nothing changes.
        if (list.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(list);
    }
}
