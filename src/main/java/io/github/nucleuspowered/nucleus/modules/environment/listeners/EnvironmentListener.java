/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.environment.listeners;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.interfaces.ListenerBase;
import io.github.nucleuspowered.nucleus.modules.environment.EnvironmentKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.world.ChangeWorldWeatherEvent;

public class EnvironmentListener implements ListenerBase {

    @Listener
    public void onWeatherChange(ChangeWorldWeatherEvent event) {
        event.setCancelled(Nucleus.getNucleus().getStorageManager()
                .getWorldService()
                .getOnThread(event.getTargetWorld().getUniqueId())
                .map(x -> x.getOrDefault(EnvironmentKeys.LOCKED_WEATHER))
                .orElse(false));
    }
}
