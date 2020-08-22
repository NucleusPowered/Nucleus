/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.listeners;

import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfig;
import io.github.nucleuspowered.nucleus.modules.core.config.WarmupConfig;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.services.interfaces.IWarmupService;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import javax.inject.Inject;

public class WarmupListener implements IReloadableService.Reloadable, ListenerBase {

    private final IWarmupService warmupService;
    private final IMessageProviderService messageProviderService;
    private WarmupConfig warmupConfig = new WarmupConfig();

    @Inject
    public WarmupListener(INucleusServiceCollection serviceCollection) {
        this.warmupService = serviceCollection.warmupService();
        this.messageProviderService = serviceCollection.messageProvider();
    }

    @Listener(order = Order.LAST)
    public void onPlayerMovement(MoveEntityEvent event, @Root Player player) {
        // Rotating is OK!
        if (this.warmupConfig.isOnMove() && !event.getFromTransform().getLocation().equals(event.getToTransform().getLocation())) {
            cancelWarmup(player);
        }
    }

    @Listener(order = Order.LAST)
    public void onPlayerCommand(SendCommandEvent event, @Root Player player) {
        if (this.warmupConfig.isOnCommand()) {
            cancelWarmup(player);
        }
    }

    @Listener(order = Order.LAST)
    public void onPlayerQuit(ClientConnectionEvent.Disconnect event) {
        cancelWarmup(event.getTargetEntity());
    }

    private void cancelWarmup(Player player) {
        if (this.warmupService.cancel(player) && player.isOnline()) {
            this.messageProviderService.sendMessageTo(player, "warmup.cancel");
        }
    }

    public void onReload(INucleusServiceCollection collection) {
        this.warmupConfig = collection.moduleDataProvider().getModuleConfig(CoreConfig.class).getWarmupConfig();
    }
}
