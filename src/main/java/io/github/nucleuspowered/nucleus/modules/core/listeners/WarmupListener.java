/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.service.NucleusWarmupManagerService;
import io.github.nucleuspowered.nucleus.internal.listeners.ListenerBase;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.core.config.WarmupConfig;
import io.github.nucleuspowered.nucleus.modules.core.events.NucleusReloadConfigEvent;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ClientConnectionEvent;

public class WarmupListener extends ListenerBase {

    private NucleusWarmupManagerService service;
    @Inject private CoreConfigAdapter cca;

    private WarmupConfig warmupConfig = null;

    @Listener(order = Order.LAST)
    public void onPlayerMovement(DisplaceEntityEvent.Move event, @Root Player player) {
        // Rotating is OK!
        if (getWarmupConfig().isOnMove() && !event.getFromTransform().getLocation().equals(event.getToTransform().getLocation())) {
            cancelWarmup(player);
        }
    }

    @Listener(order = Order.LAST)
    public void onPlayerCommand(SendCommandEvent event, @Root Player player) {
        if (getWarmupConfig().isOnCommand()) {
            cancelWarmup(player);
        }
    }

    @Listener(order = Order.LAST)
    public void onPlayerQuit(ClientConnectionEvent.Disconnect event) {
        cancelWarmup(event.getTargetEntity());
    }

    @Listener
    public void onNucleusConfigReload(NucleusReloadConfigEvent e) {
        warmupConfig = null;
    }

    private WarmupConfig getWarmupConfig() {
        if (warmupConfig == null) {
            warmupConfig = cca.getNodeOrDefault().getWarmupConfig();
        }

        return warmupConfig;
    }

    private void cancelWarmup(Player player) {
        if (service == null) {
            service = plugin.getWarmupManager();
        }

        service.cleanup();
        if (service.removeWarmup(player.getUniqueId()) && player.isOnline()) {
            player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("warmup.cancel"));
        }
    }
}
