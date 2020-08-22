/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.environment.listeners;

import io.github.nucleuspowered.nucleus.modules.environment.EnvironmentKeys;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.world.ChangeWorldWeatherEvent;

import javax.inject.Inject;

public class EnvironmentListener implements ListenerBase {

    private final INucleusServiceCollection serviceCollection;

    @Inject
    public EnvironmentListener(INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
    }

    @Listener
    public void onWeatherChange(ChangeWorldWeatherEvent event) {
        event.setCancelled(this.serviceCollection
                .storageManager()
                .getWorldService()
                .getOnThread(event.getTargetWorld().getUniqueId())
                .map(x -> x.getOrDefault(EnvironmentKeys.LOCKED_WEATHER))
                .orElse(false));
    }
}
