/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.listeners;

import io.github.nucleuspowered.nucleus.api.EventContexts;
import io.github.nucleuspowered.nucleus.modules.jail.JailPermissions;
import io.github.nucleuspowered.nucleus.modules.jail.config.JailConfig;
import io.github.nucleuspowered.nucleus.modules.jail.services.JailHandler;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;

import javax.inject.Inject;

public class InterceptTeleportListener implements ListenerBase.Conditional {

    private final JailHandler handler;
    private final IPermissionService permissionService;
    private final IMessageProviderService messageProvider;

    @Inject
    public InterceptTeleportListener(INucleusServiceCollection serviceCollection) {
        this.handler = serviceCollection.getServiceUnchecked(JailHandler.class);
        this.permissionService = serviceCollection.permissionService();
        this.messageProvider = serviceCollection.messageProvider();
    }

    @Listener(order = Order.LAST)
    public void onTeleport(MoveEntityEvent.Teleport event, @Root CommandSource cause, @Getter("getTargetEntity") Player player) {
        EventContext context = event.getCause().getContext();
        if (!context.get(EventContexts.BYPASS_JAILING_RESTRICTION).orElse(false) &&
                context.get(EventContexts.IS_JAILING_ACTION).orElse(false)) {
            if (this.handler.isPlayerJailed(player)) {
                if (!this.permissionService.hasPermission(cause, JailPermissions.JAIL_TELEPORTJAILED)) {
                    event.setCancelled(true);
                    this.messageProvider.sendMessageTo(cause, "jail.abouttoteleporttarget.isjailed", player.getName());
                } else if (!this.permissionService.hasPermission(cause, JailPermissions.JAIL_TELEPORTTOJAILED)) {
                    event.setCancelled(true);
                    this.messageProvider.sendMessageTo(cause,"jail.abouttoteleportcause.targetisjailed", player.getName());
                }
            }
        }
    }

    @Override
    public boolean shouldEnable(INucleusServiceCollection serviceCollection) {
        return serviceCollection.moduleDataProvider().getModuleConfig(JailConfig.class).aggressivelyDisableTeleportsForJailed();
    }
}
