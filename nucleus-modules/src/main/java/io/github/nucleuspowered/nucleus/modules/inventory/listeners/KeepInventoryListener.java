/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.inventory.listeners;

import io.github.nucleuspowered.nucleus.modules.inventory.InventoryPermissions;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.filter.Getter;

import javax.inject.Inject;

public class KeepInventoryListener implements ListenerBase.Conditional {

    private final IPermissionService permissionService;

    @Inject
    public KeepInventoryListener(INucleusServiceCollection serviceCollection) {
        this.permissionService = serviceCollection.permissionService();
    }

    @Listener
    public void onEntityDeath(DestructEntityEvent.Death event, @Getter("getTargetEntity") Player living) {
        if (this.permissionService.hasPermission(living, InventoryPermissions.INVENTORY_KEEP)) {
            event.setKeepInventory(true);
        }
    }

    @Override
    public boolean shouldEnable(INucleusServiceCollection serviceCollection) {
        return !serviceCollection.permissionService().isOpOnly();
    }

}
