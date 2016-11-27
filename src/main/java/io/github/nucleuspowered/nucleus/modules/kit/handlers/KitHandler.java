/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.handlers;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.data.Kit;
import io.github.nucleuspowered.nucleus.api.service.NucleusKitService;
import io.github.nucleuspowered.nucleus.argumentparsers.KitArgument;
import io.github.nucleuspowered.nucleus.configurate.datatypes.KitDataNode;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.util.Tuple;
import io.github.nucleuspowered.nucleus.dataservices.KitService;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

public class KitHandler implements NucleusKitService {

    private final Map<Container, Tuple<KitArgument.KitInfo, Inventory>> inventoryKitMap = Maps.newHashMap();
    private Tuple<Container, Inventory> firstJoinKitInventory = null;
    @Inject private KitService store;

    @Override
    public Set<String> getKitNames() {
        return store.getKits().keySet();
    }

    @Override
    public Optional<Kit> getKit(String name) {
        return Optional.ofNullable(store.getKit(name).orElse(null));
    }

    @Override
    public boolean removeKit(String kitName) {
        if (store.removeKit(kitName)) {
            store.save();
            return true;
        }

        return false;
    }

    @Override
    public synchronized void saveKit(String kitName, Kit kit) {
        Preconditions.checkArgument(kit instanceof KitDataNode);
        Util.getKeyIgnoreCase(store.getKits(), kitName).ifPresent(store::removeKit);
        store.addKit(kitName, (KitDataNode)kit);
        store.save();
    }

    @Override
    public Kit createKit() {
        return new KitDataNode();
    }

    public Optional<Tuple<KitArgument.KitInfo, Inventory>> getCurrentlyOpenInventoryKit(Container inventory) {
        return Optional.ofNullable(inventoryKitMap.get(inventory));
    }

    public boolean isOpen(String kitName) {
        return inventoryKitMap.values().stream().anyMatch(x -> x.getFirst().name.equalsIgnoreCase(kitName));
    }

    public void addKitInventoryToListener(Tuple<KitArgument.KitInfo, Inventory> kit, Container inventory) {
        Preconditions.checkState(!inventoryKitMap.containsKey(inventory));
        inventoryKitMap.put(inventory, kit);
    }

    public void removeKitInventoryFromListener(Container inventory) {
        inventoryKitMap.remove(inventory);
    }

    public Optional<Tuple<Container, Inventory>> getFirstJoinKitInventory() {
        return Optional.ofNullable(firstJoinKitInventory);
    }

    public void setFirstJoinKitInventory(@Nullable Tuple<Container, Inventory> firstJoinKitInventory) {
        this.firstJoinKitInventory = firstJoinKitInventory;
    }
}
