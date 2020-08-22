/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.freezeplayer.listeners;

import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.modules.freezeplayer.services.FreezePlayerService;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

public class FreezePlayerListener implements ListenerBase {

    private final IMessageProviderService messageProviderService;
    private final FreezePlayerService service;

    private final Map<UUID, Instant> lastFreezeNotification = Maps.newHashMap();

    @Inject
    public FreezePlayerListener(INucleusServiceCollection serviceCollection) {
        this.messageProviderService = serviceCollection.messageProvider();
        this.service = serviceCollection.getServiceUnchecked(FreezePlayerService.class);
    }

    @Listener
    public void onPlayerMovement(MoveEntityEvent event, @Root Player player) {
        event.setCancelled(checkForFrozen(player, "freeze.cancelmove"));
    }

    @Listener
    public void onPlayerInteractBlock(InteractEvent event, @Root Player player) {
        event.setCancelled(checkForFrozen(player, "freeze.cancelinteract"));
    }

    @Listener
    public void onPlayerInteractBlock(InteractBlockEvent event, @Root Player player) {
        event.setCancelled(checkForFrozen(player, "freeze.cancelinteractblock"));
    }

    @Listener
    public void onPlayerDisconnect(ClientConnectionEvent.Disconnect event) {
        this.service.invalidate(event.getTargetEntity().getUniqueId());
    }

    private boolean checkForFrozen(Player player, String message) {
        if (this.service.getFromUUID(player.getUniqueId())) {
            Instant now = Instant.now();
            if (this.lastFreezeNotification.getOrDefault(player.getUniqueId(), now).isBefore(now)) {
                this.messageProviderService.sendMessageTo(player, message);
                this.lastFreezeNotification.put(player.getUniqueId(), now.plus(2, ChronoUnit.SECONDS));
            }

            return true;
        }

        return false;
    }
}
