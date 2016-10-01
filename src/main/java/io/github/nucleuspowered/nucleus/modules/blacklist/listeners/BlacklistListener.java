/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.blacklist.listeners;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.dataservices.GeneralService;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.listeners.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.blacklist.config.BlacklistConfigAdapter;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.filter.type.Include;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BlacklistListener extends ListenerBase {

    @Inject private GeneralService store;
    @Inject private BlacklistConfigAdapter bca;

    private final String confiscateRoot = "blacklist.confiscate";
    private final String environmentRoot = "blacklist.environment";

    private final String bypass = PermissionRegistry.PERMISSIONS_PREFIX + "blacklist.bypass";
    private final Function<BlockSnapshot, ItemType> blockId = b -> b.getState().getType().getItem().orElse(ItemTypes.NONE);
    private final Function<ItemStackSnapshot, ItemType> itemId = ItemStackSnapshot::getType;

    private final Map<UUID, Instant> messageCache = Maps.newHashMap();

    @Listener
    public void onPlayerChangeItem(ChangeInventoryEvent event, @Root Player player) {
        if (bca.getNodeOrDefault().isInventory()) {
            List<ItemType> blacklistedTypes = store.getBlacklistedTypes();
            if (onTransaction(ItemStackSnapshot.class, player, event.getTransactions(), itemId, confiscateRoot)) {
                if (player.getItemInHand().isPresent() && blacklistedTypes.contains(player.getItemInHand().get().getItem())) {
                    player.setItemInHand(null);
                }
            }
        }
    }

    @Listener
    public void onPlayerDropItem(DropItemEvent.Dispense event, @Root Player player) {
        if (bca.getNodeOrDefault().isInventory()) {
            List<Transaction<Entity>> entities = event.getEntities().stream()
                    .map(x -> new Transaction<>(x, x)).collect(Collectors.toList());

            // Check for valid drops.
            if (onTransaction(Entity.class, player, entities, x -> x.get(Keys.REPRESENTED_ITEM).orElse(ItemStackSnapshot.NONE).getType(),
                    confiscateRoot)) {
                // Filter out only the valid entities.
                List<Entity> isValid = entities.stream().filter(Transaction::isValid).map(Transaction::getFinal).collect(Collectors.toList());
                event.filterEntities(isValid::contains);
            }
        }
    }

    @Listener
    public void onPlayerInteractBlock(InteractBlockEvent event, @Root Player player) {
        if (bca.getNodeOrDefault().isEnvironment()) {
            event.setCancelled(onTransaction(BlockSnapshot.class, player, new Transaction<>(event.getTargetBlock(), event.getTargetBlock()), blockId,
                    environmentRoot));
        }
    }

    @Listener
    @Include({ChangeBlockEvent.Break.class, ChangeBlockEvent.Place.class})
    public void onPlayerChangeBlock(ChangeBlockEvent event, @Root Player player) {
        if (bca.getNodeOrDefault().isEnvironment()) {
            // TODO: Temporary work-around for place event - setting the
            // transaction to invalid does nothing currently.
            if (onTransaction(BlockSnapshot.class, player, event.getTransactions(), blockId, environmentRoot)
                    && event instanceof ChangeBlockEvent.Place) {
                event.setCancelled(true);
            }
        }
    }

    private <T extends DataSerializable> boolean onTransaction(Class<T> type, Player target, Transaction<T> transaction,
            Function<T, ItemType> toIdFunction, String descRoot) {
        return onTransaction(type, target, Collections.singleton(transaction), toIdFunction, descRoot);
    }

    /**
     * Checks the provided {@link Transaction}s for blacklisted items.
     *
     * @param type The {@link Class} of type {@link T}
     * @param target The {@link Player} to check
     * @param transactions The {@link Collection} of {@link T} objects to check.
     * @param toIdFunction A function that gets the {@link ItemType} that
     *        {@link T} represents.
     * @param descRoot The root for the translation key to use.
     * @param <T> The type of {@link DataSerializable} that needs to be checked
     *        for this run.
     * @return <code>true</code> if at least one block was rejected.
     */
    @SuppressWarnings("unchecked")
    private <T extends DataSerializable> boolean onTransaction(Class<T> type, Player target, Collection<? extends Transaction<T>> transactions,
            Function<T, ItemType> toIdFunction, String descRoot) {
        if (target.hasPermission(bypass)) {
            return false;
        }

        List<ItemType> blacklistedTypes = store.getBlacklistedTypes();

        // Transactions that are blacklisted.
        List<Transaction<T>> remove = Lists.newArrayList(transactions);
        remove.removeIf(x -> !blacklistedTypes.contains(toIdFunction.apply(x.getFinal())));
        if (remove.isEmpty()) {
            return false;
        }

        // Cancel each transaction that is blacklisted.
        String item = toIdFunction.apply(remove.get(0).getFinal()).getTranslation().get();
        remove.forEach(x -> {
            if (ItemStackSnapshot.class.isAssignableFrom(type)) {
                x.setCustom(bca.getNodeOrDefault().shouldUseReplacement()
                        ? (T) ItemStack.builder().itemType(bca.getNodeOrDefault().getReplacement()).build().createSnapshot()
                        : (T) ItemStackSnapshot.NONE);
            } else {
                x.setValid(false);
            }
        });

        UUID u = target.getUniqueId();
        if (!messageCache.containsKey(u) || messageCache.get(u).isAfter(Instant.now())) {
            // Alert the user, but only once a second.
            if (remove.size() == 1) {
                target.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat(descRoot + ".single", item));
            } else {
                target.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat(descRoot + ".multiple", item));
            }

            messageCache.put(u, Instant.now().plus(1, ChronoUnit.SECONDS));

            // Cleanup stale entries.
            messageCache.entrySet().removeIf(x -> x.getValue().isAfter(Instant.now()));
        }

        return true;
    }

    @Override
    public Map<String, PermissionInformation> getPermissions() {
        Map<String, PermissionInformation> mp = Maps.newHashMap();
        mp.put(bypass, new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.blacklist.bypass"), SuggestedLevel.ADMIN));
        return mp;
    }
}
