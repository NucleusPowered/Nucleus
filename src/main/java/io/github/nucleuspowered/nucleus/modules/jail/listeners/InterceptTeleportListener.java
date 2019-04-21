/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.listeners;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.EventContexts;
import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import io.github.nucleuspowered.nucleus.internal.interfaces.ListenerBase;
import io.github.nucleuspowered.nucleus.modules.jail.commands.JailCommand;
import io.github.nucleuspowered.nucleus.modules.jail.config.JailConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.jail.services.JailHandler;
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

    private final JailHandler handler = Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(JailHandler.class);
    private final String notify;
    private final String teleport;
    private final String teleportto;

    @Inject
    public InterceptTeleportListener() {
        CommandPermissionHandler cph = Nucleus.getNucleus().getPermissionRegistry().getPermissionsForNucleusCommand(JailCommand.class);
        this.notify = cph.getPermissionWithSuffix("notify");
        this.teleport = cph.getPermissionWithSuffix("teleportjailed");
        this.teleportto = cph.getPermissionWithSuffix("teleporttojailed");
    }

    @Listener(order = Order.LAST)
    public void onTeleport(MoveEntityEvent.Teleport event, @Root CommandSource cause, @Getter("getTargetEntity") Player player) {
        EventContext context = event.getCause().getContext();
        if (!context.get(EventContexts.BYPASS_JAILING_RESTRICTION).orElse(false) &&
                context.get(EventContexts.IS_JAILING_ACTION).orElse(false)) {
            if (this.handler.isPlayerJailed(player)) {
                if (!hasPermission(cause, this.teleport)) {
                    event.setCancelled(true);
                    sendMessageTo(cause, "jail.abouttoteleporttarget.isjailed", player.getName());
                } else if (!hasPermission(cause, this.teleportto)) {
                    event.setCancelled(true);
                    sendMessageTo(cause,"jail.abouttoteleportcause.targetisjailed", player.getName());
                }
            }
        }
    }

    @Override
    public boolean shouldEnable() {
        return Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(JailConfigAdapter.class)
                .getNodeOrDefault().aggressivelyDisableTeleportsForJailed();
    }
}
