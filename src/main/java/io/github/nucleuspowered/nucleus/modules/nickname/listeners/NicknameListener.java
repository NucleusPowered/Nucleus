/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.nickname.listeners;

import io.github.nucleuspowered.nucleus.internal.interfaces.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.traits.InternalServiceManagerTrait;
import io.github.nucleuspowered.nucleus.modules.nickname.services.NicknameService;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;

import java.util.Optional;

public class NicknameListener implements ListenerBase, InternalServiceManagerTrait {

    @Listener(order = Order.FIRST)
    public void onPlayerJoin(ClientConnectionEvent.Join event, @Root Player player) {
        Optional<Text> nickname = getServiceUnchecked(NicknameService.class).getNickname(player);
        nickname.ifPresent(text -> {
            getServiceUnchecked(NicknameService.class).updateCache(player.getUniqueId(), text);
        });
        player.offer(
                Keys.DISPLAY_NAME,
                nickname.orElseGet(() -> Text.of(player.getName())));
    }

    @Listener(order = Order.LAST)
    public void onPlayerQuit(ClientConnectionEvent.Disconnect event, @Root Player player) {
        getServiceUnchecked(NicknameService.class).removeFromCache(player.getUniqueId());
    }

}
