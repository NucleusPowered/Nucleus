/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.listeners;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.events.NucleusFirstJoinEvent;
import io.github.nucleuspowered.nucleus.api.exceptions.KitRedeemException;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Kit;
import io.github.nucleuspowered.nucleus.dataservices.KitDataService;
import io.github.nucleuspowered.nucleus.internal.interfaces.ListenerBase;
import io.github.nucleuspowered.nucleus.modules.core.events.UserDataLoadedEvent;
import io.github.nucleuspowered.nucleus.modules.kit.KitKeys;
import io.github.nucleuspowered.nucleus.modules.kit.services.KitHandler;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IUserDataObject;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.filter.type.Exclude;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.item.inventory.Container;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class KitListener implements ListenerBase {

    private final KitHandler handler = getServiceUnchecked(KitHandler.class);
    private final KitDataService gds = Nucleus.getNucleus().getKitDataService();

    // For migration of the kit data.
    @SuppressWarnings("deprecation")
    @Listener
    public void onUserDataLoader(UserDataLoadedEvent event) {
        IUserDataObject dataObject = event.getDataObject();
        if (dataObject.has(KitKeys.LEGACY_KIT_LAST_USED_TIME)) {
            // migration time. We know this isn't null
            Map<String, Long> data = dataObject.getNullable(KitKeys.LEGACY_KIT_LAST_USED_TIME);
            Map<String, Instant> newData = dataObject.get(KitKeys.REDEEMED_KITS).orElseGet(HashMap::new);
            data.forEach((key, value) -> newData.putIfAbsent(key.toLowerCase(), Instant.ofEpochSecond(value)));
            dataObject.remove(KitKeys.LEGACY_KIT_LAST_USED_TIME);
            dataObject.set(KitKeys.REDEEMED_KITS, newData);
            event.save();
        }
    }

    @Listener
    public void onPlayerFirstJoin(NucleusFirstJoinEvent event, @Getter("getTargetEntity") Player player) {
        for (Kit kit : gds.getFirstJoinKits()) {
            try {
                handler.redeemKit(kit, player, true, true);
            } catch (KitRedeemException e) {
                // ignored
            }
        }
    }

    @Listener
    @Exclude({InteractInventoryEvent.Open.class})
    public void onPlayerInteractInventory(final InteractInventoryEvent event, @Root final Player player,
            @Getter("getTargetInventory") final Container inventory) {
        handler.getCurrentlyOpenInventoryKit(inventory).ifPresent(x -> {
            try {
                x.getFirst().updateKitInventory(x.getSecond());
                handler.saveKit(x.getFirst());

                if (event instanceof InteractInventoryEvent.Close) {
                    gds.save();
                    player.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.kit.edit.success", x.getFirst().getName()));
                    handler.removeKitInventoryFromListener(inventory);
                }
            } catch (Exception e) {
                if (Nucleus.getNucleus().isDebugMode()) {
                    e.printStackTrace();
                }

                player.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.kit.edit.error", x.getFirst().getName()));
            }
        });

        if (handler.isViewer(inventory)) {
            if (event instanceof InteractInventoryEvent.Close) {
                this.handler.removeViewer(inventory);
            } else {
                event.setCancelled(true);
            }
        }
    }

}
