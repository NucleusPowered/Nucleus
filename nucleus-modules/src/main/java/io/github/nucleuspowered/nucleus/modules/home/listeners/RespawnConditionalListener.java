/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.listeners;

import io.github.nucleuspowered.nucleus.api.EventContexts;
import io.github.nucleuspowered.nucleus.api.module.home.NucleusHomeService;
import io.github.nucleuspowered.nucleus.api.module.home.data.Home;
import io.github.nucleuspowered.nucleus.modules.home.config.HomeConfig;
import io.github.nucleuspowered.nucleus.modules.home.services.HomeService;
import io.github.nucleuspowered.nucleus.modules.spawn.events.SendToSpawnEvent;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.world.World;

import java.util.Optional;

import javax.inject.Inject;

public class RespawnConditionalListener implements ListenerBase.Conditional {

    private final HomeService homeService;
    private final IMessageProviderService messageProviderService;

    @Inject
    public RespawnConditionalListener(INucleusServiceCollection serviceCollection) {
        this.homeService = serviceCollection.getServiceUnchecked(HomeService.class);
        this.messageProviderService = serviceCollection.messageProvider();
    }

    @Listener
    public void onRespawn(final RespawnPlayerEvent event, @Getter("getTargetEntity") final Player player) {
        Optional<Home> oh = this.homeService.getHome(player.getUniqueId(), NucleusHomeService.DEFAULT_HOME_NAME);

        Optional<Transform<World>> ot = oh.flatMap(Home::getTransform);

        if (ot.isPresent()) {
            try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                frame.pushCause(event.getTargetEntity());
                frame.addContext(EventContexts.SPAWN_EVENT_TYPE, SendToSpawnEvent.Type.HOME_ON_DEATH);
                SendToSpawnEvent sEvent = new SendToSpawnEvent(ot.get(), event.getTargetEntity(), frame.getCurrentCause());
                if (Sponge.getEventManager().post(sEvent)) {
                    this.messageProviderService.sendMessageTo(event.getTargetEntity(), "command.home.fail", oh.get().getName());
                    return;
                }

                event.setToTransform(sEvent.getTransformTo());
            }

        }

    }

    @Override public boolean shouldEnable(INucleusServiceCollection serviceCollection) {
        return serviceCollection.moduleDataProvider().getModuleConfig(HomeConfig.class).isRespawnAtHome();
    }

}
