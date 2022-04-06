/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.EventContexts;
import io.github.nucleuspowered.nucleus.api.module.jail.data.Jailing;
import io.github.nucleuspowered.nucleus.api.teleport.event.NucleusTeleportEvent;
import io.github.nucleuspowered.nucleus.core.core.events.NucleusOnLoginEvent;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IUserDataObject;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.util.PermissionMessageChannel;
import io.github.nucleuspowered.nucleus.modules.jail.JailPermissions;
import io.github.nucleuspowered.nucleus.modules.jail.config.JailConfig;
import io.github.nucleuspowered.nucleus.modules.jail.services.JailService;
import io.github.nucleuspowered.nucleus.modules.jail.services.NucleusJailing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.command.ExecuteCommandEvent;
import org.spongepowered.api.event.entity.living.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.filter.type.Include;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.util.Nameable;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.server.ServerLocation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JailListener implements IReloadableService.Reloadable, ListenerBase {

    private final IPermissionService permissionService;
    private final IMessageProviderService messageProviderService;
    private final JailService handler;
    private List<String> allowedCommands;

    @Inject
    public JailListener(final INucleusServiceCollection serviceCollection) {
        this.permissionService = serviceCollection.permissionService();
        this.messageProviderService = serviceCollection.messageProvider();
        this.handler = serviceCollection.getServiceUnchecked(JailService.class);
    }

    // fires after spawn login event
    @Listener
    public void onPlayerLogin(final NucleusOnLoginEvent event, @Getter("getTargetUser") final User user, @Getter("getUserService") final IUserDataObject qs) {
        final Jailing jailing = this.handler.onPlayerLogin(user.uniqueId());
        if (jailing == JailService.NOT_JAILED) {
            return;
        }

        final Optional<ServerLocation> jail = this.handler.getJail(jailing.getJailName()).flatMap(x -> x.getLocation().getLocation());
        if (!jail.isPresent()) {
            new PermissionMessageChannel(this.permissionService, JailPermissions.JAIL_NOTIFY)
                    .sendMessage(Component.text("WARNING: No jail is defined for " + user.name() + " - they're going free!", NamedTextColor.RED));
            this.handler.unjailPlayer(user.uniqueId());
            return;
        }

        // always send the player back to the jail location
        event.setTo(jail.get());
    }

    /**
     * At the time the subject joins, check to see if the subject is jailed.
     *
     * @param event The event.
     */
    @Listener(order = Order.LATE)
    public void onPlayerJoin(final ServerSideConnectionEvent.Join event, @Getter("player") final ServerPlayer serverPlayer) {
        final Optional<Jailing> jailing = this.handler.getPlayerJailData(serverPlayer.uniqueId());
        if (!jailing.filter(x -> x instanceof NucleusJailing).isPresent()) {
            return;
        }

        final NucleusJailing entry = jailing.map(x -> (NucleusJailing) x).get();
        this.handler.onJail(entry, serverPlayer);
    }

    @Listener
    public void onRequestSent(final NucleusTeleportEvent.Request event, @Root final ServerPlayer cause, @Getter("getPlayer") final UUID player) {
        if (this.handler.isPlayerJailed(cause.uniqueId())) {
            event.setCancelled(true);
            event.setCancelMessage(this.messageProviderService.getMessageFor(cause.locale(), "jail.teleportcause.isjailed"));
        } else if (this.handler.isPlayerJailed(player)) {
            event.setCancelled(true);
            final String p = Sponge.server().player(player).map(Nameable::name).orElse("unknown");
            event.setCancelMessage(this.messageProviderService.getMessageFor(cause.locale(),"jail.teleporttarget.isjailed", p));
        }
    }

    @Listener
    public void onAboutToTeleport(
            final NucleusTeleportEvent.AboutToTeleport event, @Root final ServerPlayer cause, @Getter("getPlayer") final UUID player) {
        if (event.cause().context().get(EventContexts.IS_JAILING_ACTION).orElse(false)) {
            if (this.handler.isPlayerJailed(player)) {
                final String p = Sponge.server().player(player).map(Nameable::name).orElse("unknown");
                if (!this.permissionService.hasPermission(cause, JailPermissions.JAIL_TELEPORTJAILED)) {
                    event.setCancelled(true);
                    event.setCancelMessage(
                            this.messageProviderService.getMessageFor(cause, "jail.abouttoteleporttarget.isjailed", p));
                } else if (!this.permissionService.hasPermission(cause, JailPermissions.JAIL_TELEPORTTOJAILED)) {
                    event.setCancelled(true);
                    event.setCancelMessage(
                            this.messageProviderService.getMessageFor(cause,"jail.abouttoteleportcause.targetisjailed", p));
                }
            }
        }
    }

    @Listener
    public void onCommand(final ExecuteCommandEvent.Pre event, @Root final ServerPlayer player) {
        // Only if the command is not in the control list.
        if (this.handler.isPlayerJailed(player.uniqueId()) && this.allowedCommands.stream().noneMatch(x -> event.command().equalsIgnoreCase(x))) {
            event.setCancelled(true);

            // This is the easiest way to send the messages.
            this.handler.notify(player);
        }
    }

    @Listener
    public void onBlockChange(final ChangeBlockEvent.Pre event, @Root final ServerPlayer player) {
        if (this.handler.isPlayerJailed(player.uniqueId())) {
            event.setCancelled(true);
            this.handler.notify(player);
        }
    }

    // TODO: split this to appropriate events
    @Listener
    @Include(Cancellable.class)
    public void onInteract(final InteractEvent event, @Root final ServerPlayer player) {
        if (this.handler.isPlayerJailed(player.uniqueId()) && event instanceof Cancellable) {
            ((Cancellable) event).setCancelled(true);
            this.handler.notify(player);
        }
    }

    @Listener(order = Order.LAST)
    public void onSpawn(final RespawnPlayerEvent.SelectWorld event, @Getter("entity") final ServerPlayer player) {
        this.handler.getPlayerJail(player.uniqueId())
                .flatMap(x -> x.getLocation().getLocation())
                .map(Location::world)
                .ifPresent(event::setDestinationWorld);
    }

    @Listener(order = Order.LAST)
    public void onSpawn(final RespawnPlayerEvent.Recreate event, @Getter("entity") final ServerPlayer player) {
        this.handler.getPlayerJail(player.uniqueId())
                .flatMap(x -> x.getLocation().getLocation())
                .filter(x -> x.world().equals(event.destinationWorld()))
                .map(Location::position)
                .ifPresent(event::setDestinationPosition);
    }

    @Listener
    public void onLogout(final ServerSideConnectionEvent.Disconnect event, @Getter("player") final ServerPlayer serverPlayer) {
        this.handler.clearCacheFor(serverPlayer.uniqueId());
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        this.allowedCommands = serviceCollection.configProvider().getModuleConfig(JailConfig.class).getAllowedCommands();
    }
}
