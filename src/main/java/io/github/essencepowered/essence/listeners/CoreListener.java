/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.listeners;

import io.github.essencepowered.essence.internal.ListenerBase;
import io.github.essencepowered.essence.internal.interfaces.InternalEssenceUser;
import io.github.essencepowered.essence.internal.services.datastore.UserConfigLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class CoreListener extends ListenerBase {

    @Listener(order = Order.FIRST)
    public void onPlayerLogin(final ClientConnectionEvent.Login event) {
        Sponge.getScheduler().createAsyncExecutor(plugin).execute(() -> {
            try {
                InternalEssenceUser qsu = this.plugin.getUserLoader().getUser(event.getTargetUser());
                qsu.setLastLogin(Instant.now());
            } catch (IOException | ObjectMappingException e) {
                e.printStackTrace();
            }
        });
    }

    @Listener(order = Order.DEFAULT)
    public void onPlayerJoin(final ClientConnectionEvent.Join event) {
        try {
            InternalEssenceUser qsu = this.plugin.getUserLoader().getUser(event.getTargetEntity());

            // If we have a location to send them to in the config, send them there now!
            Optional<Location<World>> olw = qsu.getLocationOnLogin();
            if (olw.isPresent()) {
                event.getTargetEntity().setLocation(olw.get());
                qsu.removeLocationOnLogin();
            }
        } catch (IOException | ObjectMappingException e) {
            e.printStackTrace();
        }


    }

    @Listener
    public void onPlayerQuit(final ClientConnectionEvent.Disconnect event) {
        Sponge.getScheduler().createAsyncExecutor(plugin).schedule(() -> {
            UserConfigLoader ucl = this.plugin.getUserLoader();
            try {
                InternalEssenceUser qsu = this.plugin.getUserLoader().getUser(event.getTargetEntity());
                qsu.setOnLogout();
                ucl.purgeNotOnline();
            } catch (IOException | ObjectMappingException e) {
                e.printStackTrace();
            }
        }, 200, TimeUnit.MILLISECONDS);
    }
}
