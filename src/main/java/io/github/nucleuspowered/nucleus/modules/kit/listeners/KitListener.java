/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.listeners;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.dataservices.GeneralService;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.internal.listeners.ListenerBase;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

import javax.inject.Inject;
import java.util.List;

public class KitListener extends ListenerBase {

    @Inject private UserDataManager loader;
    @Inject private GeneralService gds;

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event) {
        Player player = event.getTargetEntity();
        final Inventory target = Util.getStandardInventory(player);
        loader.get(player).ifPresent(p -> {
            if (p.isFirstPlay()) {
                List<ItemStackSnapshot> l = gds.getFirstKit();
                if (l != null && !l.isEmpty()) {
                    l.stream().filter(x -> x.getType() != ItemTypes.NONE).forEach(x -> target.offer(x.createStack()));
                }
            }
        });
    }
}
