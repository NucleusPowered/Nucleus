/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.back.listeners;

import io.github.nucleuspowered.nucleus.api.module.jail.NucleusJailService;
import io.github.nucleuspowered.nucleus.modules.back.BackPermissions;
import io.github.nucleuspowered.nucleus.modules.back.config.BackConfig;
import io.github.nucleuspowered.nucleus.modules.back.services.BackHandler;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.type.Exclude;

import javax.annotation.Nullable;
import javax.inject.Inject;

public class BackListeners implements IReloadableService.Reloadable, ListenerBase {

    private final BackHandler handler;
    private final IPermissionService permissionService;
    private BackConfig backConfig = new BackConfig();
    @Nullable private final NucleusJailService jailService;

    @Inject
    public BackListeners(INucleusServiceCollection serviceCollection) {
        this.jailService = Sponge.getServiceManager().provide(NucleusJailService.class).orElse(null);
        this.handler = serviceCollection.getServiceUnchecked(BackHandler.class);
        this.permissionService = serviceCollection.permissionService();
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        this.backConfig = serviceCollection.moduleDataProvider().getModuleConfig(BackConfig.class);
    }

    @Listener
    @Exclude(MoveEntityEvent.Teleport.Portal.class) // Don't set /back on a portal.
    public void onTeleportPlayer(MoveEntityEvent.Teleport event, @Getter("getTargetEntity") Player pl) {
        if (this.backConfig.isOnTeleport() && check(event) && getLogBack(pl) && this.permissionService.hasPermission(pl, BackPermissions.BACK_ONTELEPORT)) {
            this.handler.setLastLocation(pl, event.getFromTransform());
        }
    }

    @Listener
    public void onPortalPlayer(MoveEntityEvent.Teleport.Portal event, @Getter("getTargetEntity") Player pl) {
        if (this.backConfig.isOnPortal() && check(event) && getLogBack(pl)  && this.permissionService.hasPermission(pl, BackPermissions.BACK_ONPORTAL)) {
            this.handler.setLastLocation(pl, event.getFromTransform());
        }
    }

    @Listener
    public void onDeathEvent(DestructEntityEvent.Death event) {
        Living e = event.getTargetEntity();
        if (!(e instanceof Player)) {
            return;
        }

        Player pl = (Player)e;
        if (this.backConfig.isOnDeath() && getLogBack(pl) && this.permissionService.hasPermission(pl, BackPermissions.BACK_ONDEATH)) {
            this.handler.setLastLocation(pl, event.getTargetEntity().getTransform());
        }
    }

    private boolean check(MoveEntityEvent.Teleport event) {
        return !event.getFromTransform().equals(event.getToTransform());
    }

    private boolean getLogBack(Player player) {
        return !(this.jailService != null && this.jailService.isPlayerJailed(player)) && this.handler.isLoggingLastLocation(player);
    }
}
