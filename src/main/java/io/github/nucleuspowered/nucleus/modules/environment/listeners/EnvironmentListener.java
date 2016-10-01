/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.environment.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.data.NucleusWorld;
import io.github.nucleuspowered.nucleus.dataservices.loaders.WorldDataManager;
import io.github.nucleuspowered.nucleus.internal.listeners.ListenerBase;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.world.ChangeWorldWeatherEvent;

public class EnvironmentListener extends ListenerBase {

    @Inject private WorldDataManager loader;
    @Inject private CoreConfigAdapter cca;

    @Listener
    public void onWeatherChange(ChangeWorldWeatherEvent event) {
        try {
            NucleusWorld ew = loader.getWorld(event.getTargetWorld()).get();
            event.setCancelled(ew.isLockWeather());
        } catch (Exception e) {
            if (cca.getNodeOrDefault().isDebugmode()) {
                e.printStackTrace();
            }
        }
    }
}
