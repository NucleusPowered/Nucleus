/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.staffchat.listener;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.internal.listeners.ListenerBase;
import io.github.nucleuspowered.nucleus.modules.staffchat.StaffChatMessageChannel;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.message.MessageChannelEvent;

public class StaffChatListener extends ListenerBase {

    @Inject private UserDataManager loader;

    @Listener(order = Order.FIRST)
    public void onMessage(MessageChannelEvent.Chat event, @Root Player player) {
        if (inAdminChat(player)) {
            event.setMessage(event.getRawMessage());
            event.setChannel(StaffChatMessageChannel.getInstance());
        }
    }

    private boolean inAdminChat(Player player) {
        try {
            return loader.get(player).get().isInStaffChat();
        } catch (Exception e) {
            return false;
        }
    }
}
