/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.blacklist.listeners;

import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.config.GeneralDataStore;
import io.github.nucleuspowered.nucleus.internal.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.manipulator.mutable.RepresentedItemData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;

import java.util.Map;
import java.util.Optional;

public class BlacklistListener extends ListenerBase {

    private final String prefix = PermissionRegistry.PERMISSIONS_PREFIX + "blacklist.";

    @Listener
    public void onPlayerChangeHeldItem(ChangeInventoryEvent.Held event, @First Player player) {
        for (SlotTransaction transaction : event.getTransactions()) {
            if (checkForBlacklist(player, transaction.getFinal().createStack().getItem().getId(), true)) {
                transaction.setCustom(ItemStack.builder().itemType(ItemTypes.DIRT).quantity(1).build());
            }
        }
    }

    @Listener
    public void onPlayerChangeEquipment(ChangeInventoryEvent.Equipment event, @First Player player) {
        for (SlotTransaction transaction : event.getTransactions()) {
            if (checkForBlacklist(player, transaction.getFinal().createStack().getItem().getId(), true)) {
                transaction.setCustom(ItemStack.builder().itemType(ItemTypes.DIRT).quantity(1).build());
            }
        }
    }

    @Listener
    public void onPlayerDropItem(DropItemEvent.Dispense event, @First Player player) {
        event.filterEntities(e -> {
            if (e.get(RepresentedItemData.class).isPresent()) {
                RepresentedItemData itemData = e.get(RepresentedItemData.class).get();
                return !checkForBlacklist(player, itemData.item().get().getType().getId(), true);
            }
            return false;
        });
    }

    @Listener
    public void onPlayerPickupItem(ChangeInventoryEvent.Pickup event, @First Player player) {
        for (SlotTransaction transaction : event.getTransactions()) {
            if (checkForBlacklist(player, transaction.getFinal().createStack().getItem().getId(), true)) {
                transaction.setCustom(ItemStack.builder().itemType(ItemTypes.DIRT).quantity(1).build());
            }
        }
    }

    @Listener
    public void onPlayerInteractBlock(InteractBlockEvent event, @First Player player) {
        event.setCancelled(checkForBlacklist(player, event.getTargetBlock().getState().getType().getId(), false));
    }

    @Listener
    public void onPlayerPlaceBlock(ChangeBlockEvent.Place event, @First Player player) {
        for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
            event.setCancelled(checkForBlacklist(player, transaction.getFinal().getState().getType().getId(), false));
        }
    }

    @Listener
    public void onPlayerBreakBlock(ChangeBlockEvent.Break event, @First Player player) {
        for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
            event.setCancelled(checkForBlacklist(player, transaction.getOriginal().getState().getType().getId(), false));
        }
    }

    private boolean checkForBlacklist(Player player, String id, boolean shouldMessage) {
        GeneralDataStore dataStore = this.plugin.getGeneralDataStore();
        Optional<ItemType> blacklistedItem = dataStore.getBlacklistedTypes().stream().filter(i -> i.getId().equals(id)).findAny();

        if (blacklistedItem.isPresent() && !player.hasPermission(this.prefix + "bypass")) {
            if (shouldMessage) {
                player.sendMessage(Util.getTextMessageWithFormat("blacklist.confiscate", blacklistedItem.get().getTranslation().get()));
            }

            return true;
        }

        return false;
    }

    @Override
    protected Map<String, PermissionInformation> getPermissions() {
        Map<String, PermissionInformation> mp = Maps.newHashMap();
        mp.put(prefix + "bypass", new PermissionInformation(Util.getMessageWithFormat("permission.blacklist.bypass"), SuggestedLevel.ADMIN));
        return super.getPermissions();
    }
}
