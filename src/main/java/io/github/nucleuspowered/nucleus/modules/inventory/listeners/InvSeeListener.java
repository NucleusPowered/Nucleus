/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.inventory.listeners;

import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import io.github.nucleuspowered.nucleus.internal.ListenerBase;
import io.github.nucleuspowered.nucleus.modules.inventory.commands.InvSeeCommand;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.filter.type.Exclude;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.type.CarriedInventory;

public class InvSeeListener extends ListenerBase {

    private CommandPermissionHandler invSeePermissionHandler = null;

    /**
     * Fired when a {@link Player} interacts with another's inventory.
     *
     * @param event The {@link ClickInventoryEvent} event to handle.
     * @param player The {@link Player} that is doing the interacting.
     * @param targetInventory The {@link CarriedInventory} that is being interacted with.
     */
    @Listener
    @Exclude({ClickInventoryEvent.Open.class, ClickInventoryEvent.Close.class})
    public void onInventoryChange(ClickInventoryEvent event, @Root Player player, @Getter("getTargetInventory") Inventory targetInventory) {
        if (!(targetInventory instanceof CarriedInventory)) {
            return;
        }

        CarriedInventory<?> carriedInventory = (CarriedInventory)targetInventory;
        if (!carriedInventory.getCarrier().isPresent() || !(carriedInventory.getCarrier().get() instanceof Player)) {
            return;
        }

        Player target = (Player)carriedInventory.getCarrier().get();
        if (player.equals(target)) {
            return;
        }

        if (this.invSeePermissionHandler == null) {
            this.invSeePermissionHandler = plugin.getPermissionRegistry().getService(InvSeeCommand.class);
        }

        // Ok, so we're interacting with another player's inventory.
        if (invSeePermissionHandler.testSuffix(target, "exempt.interact")) {
            event.getCursorTransaction().setValid(false);
            event.setCancelled(true);
        }
    }
}
