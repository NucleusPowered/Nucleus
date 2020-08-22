/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.afk.listeners;

import io.github.nucleuspowered.nucleus.modules.afk.config.AFKConfig;
import io.github.nucleuspowered.nucleus.modules.afk.services.AFKHandler;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.RotateEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.world.World;

import javax.inject.Inject;

public class AFKRotationOnlyListener extends AbstractAFKListener implements ListenerBase.Conditional {

    @Inject
    public AFKRotationOnlyListener(INucleusServiceCollection serviceCollection) {
        super(serviceCollection.getServiceUnchecked(AFKHandler.class));
    }

    @Listener(order = Order.LAST)
    public void onPlayerMove(final RotateEntityEvent event, @Root Player player,
            @Getter("getFromTransform") Transform<World> from,
            @Getter("getToTransform") Transform<World> to) {
        if (!from.getRotation().equals(to.getRotation())) {
            update(player);
        }
    }

    @Override
    public boolean shouldEnable(INucleusServiceCollection serviceCollection) {
        AFKConfig.Triggers triggers = serviceCollection.moduleDataProvider().getModuleConfig(AFKConfig.class)
                .getTriggers();
        return triggers.isOnMovement() && !triggers.isOnRotation();
    }

}
