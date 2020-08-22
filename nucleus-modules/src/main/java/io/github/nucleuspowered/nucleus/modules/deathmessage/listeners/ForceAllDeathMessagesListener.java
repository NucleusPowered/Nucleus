/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.deathmessage.listeners;

import io.github.nucleuspowered.nucleus.modules.deathmessage.config.DeathMessageConfig;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.text.channel.MessageChannel;

public class ForceAllDeathMessagesListener implements ListenerBase.Conditional {

    @Listener(order = Order.LATE)
    public void onDeath(DestructEntityEvent.Death event, @Getter("getTargetEntity") Living living) {
        if (living instanceof Player) {
            event.setChannel(MessageChannel.TO_ALL);
        }
    }

    @Override public boolean shouldEnable(INucleusServiceCollection serviceCollection) {
        DeathMessageConfig deathMessageConfig = serviceCollection.moduleDataProvider().getModuleConfig(DeathMessageConfig.class);
        return deathMessageConfig.isEnableDeathMessages() && deathMessageConfig.isForceForAll();
    }

}