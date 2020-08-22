/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.afk.listeners;

import io.github.nucleuspowered.nucleus.api.module.afk.event.NucleusAFKEvent;
import io.github.nucleuspowered.nucleus.modules.afk.AFKPermissions;
import io.github.nucleuspowered.nucleus.modules.afk.config.AFKConfig;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.format.TextColors;

import javax.inject.Inject;

public class AFKSpectatorListener implements ListenerBase.Conditional {

    private final IPermissionService permissionService;

    @Inject
    public AFKSpectatorListener(IPermissionService permissionService) {
        this.permissionService = permissionService;
    }


    @Listener
    public void onAfk(NucleusAFKEvent event, @Getter("getTargetEntity") Player player) {
        if (player.gameMode().get().equals(GameModes.SPECTATOR)) {
            if (event.getChannel() == MessageChannel.TO_ALL) {
                event.setChannel(this.permissionService.permissionMessageChannel(AFKPermissions.AFK_NOTIFY));
                event.setMessage(Text.of(TextColors.YELLOW, "[Spectator] ", event.getMessage()));
            }
        }
    }

    @Listener(order = Order.FIRST)
    public void onAfk(NucleusAFKEvent.Kick event, @Getter("getTargetEntity") Player player) {
        if (player.gameMode().get().equals(GameModes.SPECTATOR)) {
            event.setCancelled(true);
        }
    }

    @Override
    public boolean shouldEnable(INucleusServiceCollection serviceCollection) {
        return serviceCollection.moduleDataProvider().getModuleConfig(AFKConfig.class).isDisableInSpectatorMode();
    }
}
