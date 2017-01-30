/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.inventory.inventory;

import io.github.nucleuspowered.nucleus.Nucleus;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.filter.type.Exclude;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.property.InventoryDimension;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.item.inventory.type.GridInventory;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;

import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

public final class InvSeeInventoryFactory {

    private InvSeeInventoryFactory() {}

    public static boolean viewInventory(Nucleus plugin, Player viewer, User source, boolean viewOnly) {

        Inventory clone = Inventory.builder().of(InventoryArchetypes.DOUBLE_CHEST)
            .property(InventoryTitle.PROPERTY_NAME, InventoryTitle.of(Text.of(source.getName())))
            .property(InventoryDimension.PROPERTY_NAM, InventoryDimension.of(9, 4))
            .withCarrier(viewer)
            .build(plugin);

        Sponge.getEventManager().registerListeners(plugin, new InventoryListener(viewer.getUniqueId(), source.getUniqueId(), clone, viewOnly));

        viewer.openInventory(clone, Cause.of(NamedCause.of("plugin", plugin), NamedCause.source(viewer)));
        return true;
    }

    public static class InventoryListener {

        private final Inventory viewedInventory;
        private final UUID target;
        private final UUID viewer;
        private final boolean viewOnly;

        private InventoryListener(UUID viewer, UUID target, Inventory viewedInventory, boolean viewOnly) {
            this.viewer = viewer;
            this.target = target;
            this.viewedInventory = viewedInventory;
            this.viewOnly = viewOnly;
        }

        @Listener(order = Order.LAST)
        @Exclude({ClickInventoryEvent.Open.class, ClickInventoryEvent.Close.class})
        public void onInventoryChange(ChangeInventoryEvent event, @Root Player interactingPlayer, @Getter("getTargetInventory") Inventory inventory) {
            if (interactingPlayer.getUniqueId().equals(target) && inventory instanceof CarriedInventory) {
                CarriedInventory<?> carriedInventory = (CarriedInventory<?>) inventory;
                if (carriedInventory.getCarrier().isPresent() && carriedInventory.getCarrier().get() instanceof User) {
                    if (viewOnly) {
                        event.setCancelled(true);
                    } else {
                        User user = (User) carriedInventory.getCarrier().get();
                        if (user.getUniqueId().equals(target)) {
                            syncInventories(carriedInventory.query(Hotbar.class, GridInventory.class), viewedInventory);
                        }
                    }
                }
            } else if (interactingPlayer.getUniqueId().equals(viewer) && viewedInventory.equals(inventory)) {
                Optional<Player> player = Sponge.getServer().getPlayer(target);
                Inventory targetInventory;
                if (player.isPresent()) {
                    targetInventory = player.get().getInventory();
                } else {
                    targetInventory = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(target).get().getInventory();
                }

                syncInventories(viewedInventory, targetInventory.query(Hotbar.class, GridInventory.class));
            }
        }

        private void syncInventories(Inventory from, Inventory to) {
            // Set the new inventory.
            Iterator<Inventory> invToEmulate = from.slots().iterator();
            Iterator<Inventory> thisInv = to.slots().iterator();

            while (invToEmulate.hasNext() && thisInv.hasNext()) {
                Inventory slotToAdd = invToEmulate.next();
                Optional<ItemStack> ois = thisInv.next().peek();
                Optional<ItemStack> oithat = slotToAdd.peek();
                if (ois.isPresent()) {
                    if (oithat.isPresent() && !oithat.get().equalTo(ois.get())) {
                        slotToAdd.set(ois.get().copy());
                    }
                } else {
                    slotToAdd.clear();
                }
            }
        }

        @Listener
        public void onInventoryClose(ClickInventoryEvent.Close event, @Root Player player) {
            if (player.getUniqueId().equals(viewer)) {
                Sponge.getEventManager().unregisterListeners(this);
            }
        }
    }
}
