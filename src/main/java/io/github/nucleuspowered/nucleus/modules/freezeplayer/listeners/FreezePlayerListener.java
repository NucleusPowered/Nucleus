/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.freezeplayer.listeners;

import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.interfaces.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.traits.InternalServiceManagerTrait;
import io.github.nucleuspowered.nucleus.modules.freezeplayer.service.FreezeService;
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

public class FreezePlayerListener implements ListenerBase, InternalServiceManagerTrait {

    private final Map<UUID, Instant> lastFreezeNotification = Maps.newHashMap();
    private final FreezeService freezeService = Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(FreezeService.class);

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
    public void onPlayerLeave(ClientConnectionEvent.Disconnect event, @Root Player player) {
        this.freezeService.invalidate(player.getUniqueId());
    }

    private boolean checkForFrozen(Player player, String message) {
        if (this.freezeService.getFromUUID(player.getUniqueId())) {
            Instant now = Instant.now();
            if (this.lastFreezeNotification.getOrDefault(player.getUniqueId(), now).isBefore(now)) {
                player.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat(message));
                this.lastFreezeNotification.put(player.getUniqueId(), now.plus(2, ChronoUnit.SECONDS));
            }

            return true;
        }

        return false;
    }
}
