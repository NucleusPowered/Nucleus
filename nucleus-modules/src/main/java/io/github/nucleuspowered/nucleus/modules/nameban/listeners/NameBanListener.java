/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.nameban.listeners;

import io.github.nucleuspowered.nucleus.modules.nameban.services.NameBanHandler;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.serializer.TextSerializers;

import javax.inject.Inject;

public class NameBanListener implements ListenerBase {

    private final NameBanHandler nameBanHandler;

    @Inject
    public NameBanListener(INucleusServiceCollection serviceCollection) {
        this.nameBanHandler = serviceCollection.getServiceUnchecked(NameBanHandler.class);
    }

    @Listener
    public void onPlayerLogin(ClientConnectionEvent.Auth event) {
        event.getProfile().getName().flatMap(name -> this.nameBanHandler.getReasonForBan(name.toLowerCase())).ifPresent(x -> {
            event.setCancelled(true);
            event.setMessage(TextSerializers.FORMATTING_CODE.deserialize(x));
        });
    }
}
