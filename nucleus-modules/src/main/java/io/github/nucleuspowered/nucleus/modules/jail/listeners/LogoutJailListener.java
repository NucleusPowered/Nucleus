/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.listeners;

import io.github.nucleuspowered.nucleus.modules.jail.config.JailConfig;
import io.github.nucleuspowered.nucleus.modules.jail.services.JailHandler;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import javax.inject.Inject;

public class LogoutJailListener implements ListenerBase.Conditional {

    private final JailHandler handler;

    @Inject
    public LogoutJailListener(INucleusServiceCollection serviceCollection) {
        this.handler = serviceCollection.getServiceUnchecked(JailHandler.class);
    }

    @Listener
    public void onLogout(ClientConnectionEvent.Disconnect event, @Getter("getTargetEntity") Player player) {
        this.handler.getPlayerJailDataInternal(player)
                    .ifPresent(jailData -> {
                Optional<Instant> end = jailData.getEndTimestamp();
                end.ifPresent(instant -> jailData.setTimeFromNextLogin(Duration.between(Instant.now(), instant)));
                this.handler.updateJailData(player, jailData);
            });
    }

    @Override public boolean shouldEnable(INucleusServiceCollection serviceCollection) {
        return serviceCollection.moduleDataProvider().getModuleConfig(JailConfig.class).isJailOnlineOnly();
    }

}
