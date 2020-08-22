/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.listeners;

import io.github.nucleuspowered.nucleus.api.core.event.NucleusFirstJoinEvent;
import io.github.nucleuspowered.nucleus.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.modules.spawn.SpawnKeys;
import io.github.nucleuspowered.nucleus.modules.spawn.config.SpawnConfig;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IStorageManager;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;

import javax.inject.Inject;

public class FirstSpawnConditionalListener implements ListenerBase.Conditional {

    private final IStorageManager storageManager;
    private final PluginContainer pluginContainer;

    @Inject
    public FirstSpawnConditionalListener(INucleusServiceCollection serviceCollection) {
        this.storageManager = serviceCollection.storageManager();
        this.pluginContainer = serviceCollection.pluginContainer();
    }

    @Listener(order = Order.LATE)
    public void onJoin(NucleusFirstJoinEvent event, @Getter("getTargetEntity") Player player) {
        // Try to force a subject location in a tick.
        Task.builder().execute(() -> this.storageManager
                .getGeneralService()
                .getOrNew()
                .join()
                .get(SpawnKeys.FIRST_SPAWN_LOCATION)
                .flatMap(LocationNode::getTransformIfExists)
                .ifPresent(player::setTransform))
                .delayTicks(3)
                .submit(this.pluginContainer);
    }

    @Override public boolean shouldEnable(INucleusServiceCollection serviceCollection) {
        return serviceCollection.moduleDataProvider().getModuleConfig(SpawnConfig.class).isForceFirstSpawn();
    }

}
