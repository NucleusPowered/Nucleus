/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.inventory.listeners;

import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.filter.type.Exclude;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.type.CarriedInventory;

import java.util.Map;
import java.util.UUID;

public class InvSeeListener implements ListenerBase {

    private static Map<UUID, Inventory> preventModify = Maps.newHashMap();

    public static void addEntry(UUID uuid, Container inventory) {
        preventModify.put(uuid, inventory);
    }

    /**
     * Fired when a {@link Player} interacts with another's inventory.
     *
     * @param event The {@link ClickInventoryEvent} event to handle.
     * @param player The {@link Player} that is doing the interacting.
     * @param targetInventory The {@link CarriedInventory} that is being interacted with.
     */
    @Listener
    @Exclude({InteractInventoryEvent.Open.class, InteractInventoryEvent.Close.class})
    public void onInventoryChange(InteractInventoryEvent event, @First Player player, @Getter("getTargetInventory") Container targetInventory) {

        if (preventModify.get(player.getUniqueId()) == targetInventory) {
            event.setCancelled(true);
            event.getCursorTransaction().setValid(false);
        }

    }

    @Listener(order = Order.POST)
    public void onInventoryClose(InteractInventoryEvent.Close event, @First Player player, @Getter("getTargetInventory") Container targetInventory) {
        if (preventModify.get(player.getUniqueId()) == targetInventory) {
            preventModify.remove(player.getUniqueId());
        }
    }

    @Listener
    public void onLogout(ClientConnectionEvent.Disconnect event, @Root Player player) {
        preventModify.remove(player.getUniqueId());
    }

}
