/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.listeners;

import com.google.inject.Inject;
import io.github.essencepowered.essence.api.PluginModule;
import io.github.essencepowered.essence.api.data.EssenceWorld;
import io.github.essencepowered.essence.internal.ListenerBase;
import io.github.essencepowered.essence.internal.annotations.Modules;
import io.github.essencepowered.essence.internal.services.datastore.WorldConfigLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.world.ChangeWorldWeatherEvent;

import java.io.IOException;

@Modules(PluginModule.ENVIRONMENT)
public class EnvironmentListener extends ListenerBase {

    @Inject private WorldConfigLoader loader;

    @Listener
    public void onWeatherChange(ChangeWorldWeatherEvent event) {
        try {
            EssenceWorld ew = loader.getWorld(event.getTargetWorld());
            event.setCancelled(ew.isLockWeather());
        } catch (IOException | ObjectMappingException e) {
            e.printStackTrace();
        }
    }
}
