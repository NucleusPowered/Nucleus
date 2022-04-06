/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.module.home.NucleusHomeService;
import io.github.nucleuspowered.nucleus.api.module.home.data.Home;
import io.github.nucleuspowered.nucleus.api.util.data.NamedLocation;
import io.github.nucleuspowered.nucleus.modules.home.config.HomeConfig;
import io.github.nucleuspowered.nucleus.modules.home.services.HomeService;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.living.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Optional;

public class RespawnConditionalListener implements ListenerBase.Conditional {

    private final HomeService homeService;

    @Inject
    public RespawnConditionalListener(final INucleusServiceCollection serviceCollection) {
        this.homeService = serviceCollection.getServiceUnchecked(HomeService.class);
    }

    @Listener
    public void onRespawn(final RespawnPlayerEvent.SelectWorld event, @Getter("entity") final ServerPlayer player) {
        final Optional<Home> oh = this.homeService.getHome(player.uniqueId(), NucleusHomeService.DEFAULT_HOME_NAME);
        oh.flatMap(x -> x.getLocation().getWorld()).ifPresent(event::setDestinationWorld);
    }

    @Listener
    public void onRespawn(final RespawnPlayerEvent.Recreate event, @Getter("entity") final ServerPlayer player) {
        final Optional<Home> oh = this.homeService.getHome(player.uniqueId(), NucleusHomeService.DEFAULT_HOME_NAME);
        oh.map(x -> x.getLocation().getPosition()).ifPresent(event::setDestinationPosition);
    }

    @Override public boolean shouldEnable(final INucleusServiceCollection serviceCollection) {
        return serviceCollection.configProvider().getModuleConfig(HomeConfig.class).isRespawnAtHome();
    }

}
