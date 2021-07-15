/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.connection.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.modules.connection.ConnectionPermissions;
import io.github.nucleuspowered.nucleus.modules.connection.config.ConnectionConfig;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.service.ban.Ban;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.util.Tristate;

import java.util.Optional;

public class ConnectionListener implements IReloadableService.Reloadable, ListenerBase {

    private final IPermissionService permissionService;

    private int reservedSlots = 0;
    @Nullable private Component whitelistMessage;
    @Nullable private Component fullMessage;

    @Inject
    public ConnectionListener(final IPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * Perform connection events on when a player is currently not permitted to join.
     *
     * @param event The event.
     */
    @Listener(order = Order.FIRST)
    @IsCancelled(Tristate.TRUE)
    public void onPlayerJoinAndCancelled(final ServerSideConnectionEvent.Login event, @Getter("user") final User user) {
        // Don't affect the banned.
        // Have to join - we need to handle this event
        // TODO: Maybe look into getting Sponge to report a ban in this event if there is one.
        final BanService banService = Sponge.server().serviceProvider().banService();
        final Optional<Ban.Profile> profileBan = banService.find(user.profile()).join();
        if (profileBan.isPresent()) {
            return;
        }

        final Optional<Ban.IP> ipBan = banService.find(event.connection().address().getAddress()).join();
        if (ipBan.isPresent()) {
            return;
        }

        if (Sponge.server().isWhitelistEnabled()
            && !Sponge.server().serviceProvider().whitelistService().isWhitelisted(user.profile()).join()) {
            if (this.whitelistMessage != null) {
                event.setMessage(this.whitelistMessage);
                event.setCancelled(true);
            }

            // Do not continue, whitelist should always apply.
            return;
        }

        final int slotsLeft = Sponge.server().maxPlayers() - Sponge.server().onlinePlayers().size();
        if (slotsLeft <= 0) {
            if (this.permissionService.hasPermission(user, ConnectionPermissions.CONNECTION_JOINFULLSERVER)) {

                // That minus sign before slotsLeft is not a typo. Leave it be!
                // It will be negative, reserved slots is positive - need to account for that.
                if (this.reservedSlots <= -1 || -slotsLeft < this.reservedSlots) {
                    event.setCancelled(false);
                    return;
                }
            }

            if (this.fullMessage != null) {
                event.setMessage(this.fullMessage);
            }
        }

    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        final ConnectionConfig connectionConfig = serviceCollection.configProvider().getModuleConfig(ConnectionConfig.class);
        this.reservedSlots = connectionConfig.getReservedSlots();
        this.whitelistMessage = connectionConfig.getWhitelistMessage().orElse(null);
        this.fullMessage = connectionConfig.getServerFullMessage().orElse(null);
    }

}
