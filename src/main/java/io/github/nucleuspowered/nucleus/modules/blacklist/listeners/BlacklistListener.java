/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.blacklist.listeners;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.configurate.datatypes.item.BlacklistNode;
import io.github.nucleuspowered.nucleus.dataservices.ItemDataService;
import io.github.nucleuspowered.nucleus.internal.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.blacklist.config.BlacklistConfigAdapter;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandType;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.filter.type.Include;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.item.inventory.UseItemStackEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.text.translation.Translatable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public class BlacklistListener extends ListenerBase {

    @Inject private ItemDataService itemDataService;
    @Inject private BlacklistConfigAdapter bca;

    private final BlacklistNode blacklistNode = new BlacklistNode();
    private final String confiscateRoot = "blacklist.confiscate";
    private final String environmentRoot = "blacklist.environment";
    private final String useRoot = "blacklist.use";

    private final String bypass = PermissionRegistry.PERMISSIONS_PREFIX + "blacklist.bypass";
    private final String use = PermissionRegistry.PERMISSIONS_PREFIX + "blacklist.bypass.use";
    private final String possess = PermissionRegistry.PERMISSIONS_PREFIX + "blacklist.bypass.possesion";
    private final String environment = PermissionRegistry.PERMISSIONS_PREFIX + "blacklist.bypass.environment";

    private final Function<BlockSnapshot, CatalogType> blockId = BlockSnapshot::getExtendedState;
    private final Function<BlockSnapshot, CatalogType> blockId2 = x -> x.getExtendedState().getType();
    private final Function<ItemStackSnapshot, CatalogType> itemId = ItemStackSnapshot::getType;

    private final Map<UUID, Instant> messageCache = Maps.newHashMap();

    @Listener
    public void onPlayerChangeItem(ChangeInventoryEvent event, @Root Player player) {
        if (onTransaction(ItemStackSnapshot.class, player, event.getTransactions(), Transaction::getFinal, itemId, null, confiscateRoot, possess, BlacklistNode::isInventory)) {
            player.getItemInHand(HandTypes.MAIN_HAND).ifPresent(x -> checkHand(HandTypes.MAIN_HAND, player, x));
            player.getItemInHand(HandTypes.OFF_HAND).ifPresent(x -> checkHand(HandTypes.OFF_HAND, player, x));
        }
    }

    @Listener
    public void onPlayerUseItem(UseItemStackEvent event, @Root Player player) {
        onTransaction(ItemStackSnapshot.class, player, new Transaction<>(event.getItemStackInUse(), event.getItemStackInUse()), Transaction::getOriginal, itemId, null, useRoot, use, BlacklistNode::isUse);
    }

    @Listener
    public void onPlayerDropItem(DropItemEvent.Dispense event, @Root Player player) {
        List<Transaction<Entity>> entities = event.getEntities().stream()
                .map(x -> new Transaction<>(x, x)).collect(Collectors.toList());

        // Check for valid drops.
        if (onTransaction(Entity.class, player, entities, Transaction::getOriginal, x -> x.get(Keys.REPRESENTED_ITEM).orElse(ItemStackSnapshot.NONE).getType(),
                null, confiscateRoot, possess, BlacklistNode::isInventory)) {
            // Filter out only the valid entities.
            List<Entity> isValid = entities.stream().filter(Transaction::isValid).map(Transaction::getFinal).collect(Collectors.toList());
            event.filterEntities(isValid::contains);
        }
    }

    @Listener
    public void onPlayerInteractBlock(InteractBlockEvent event, @Root Player player) {
        event.setCancelled(onTransaction(BlockSnapshot.class, player, new Transaction<>(event.getTargetBlock(), event.getTargetBlock()), Transaction::getFinal, blockId,
                blockId2, environmentRoot, environment, BlacklistNode::isUse));
    }

    @Listener
    @Include({ChangeBlockEvent.Break.class, ChangeBlockEvent.Place.class})
    public void onPlayerChangeBlock(ChangeBlockEvent event, @Root Player player) {
        Function<Transaction<BlockSnapshot>, BlockSnapshot> t = event instanceof ChangeBlockEvent.Break ? Transaction::getOriginal : Transaction::getFinal;
        // TODO: Temporary work-around for place event - setting the transaction to invalid does nothing currently.
        if (onTransaction(BlockSnapshot.class, player, event.getTransactions(), t, blockId, blockId2, environmentRoot, environment, BlacklistNode::isEnvironment)) {
            event.setCancelled(true);
        }
    }

    private <T extends DataSerializable> boolean onTransaction(Class<T> type, Player target, Transaction<T> transaction,
            Function<Transaction<T>, T> transactionPartToCheck, Function<T, CatalogType> toIdFunction, Function<T, CatalogType> toIdFunction2,
            String descRoot, String permissionToCheck, Predicate<BlacklistNode> check) {
        return onTransaction(type, target, Collections.singleton(transaction), transactionPartToCheck, toIdFunction, toIdFunction2, descRoot, permissionToCheck, check);
    }

    /**
     * Checks the provided {@link Transaction}s for blacklisted items.
     *
     * @param type The {@link Class} of type {@link T}
     * @param target The {@link Player} to check
     * @param transactions The {@link Collection} of {@link T} objects to check.
     * @param transactionPartToCheck The {@link Transaction} method to check.
     * @param toIdFunction A function that gets the {@link CatalogType} that
     *        {@link T} represents.
     * @param toIdFunction A function that gets the {@link CatalogType } that
     *        {@link T} represents, generally an {@link ItemType}. May be null.
     * @param descRoot The root for the translation key to use.
     * @param permissionToCheck The action specific permission to check.
     * @param check The check to perform on the {@link BlacklistNode} to see if the action is indeed banned.
     * @param <T> The type of {@link DataSerializable} that needs to be checked
     *        for this run.
     * @return <code>true</code> if at least one block was rejected.
     */
    @SuppressWarnings("unchecked")
    private <T extends DataSerializable> boolean onTransaction(Class<T> type, Player target, Collection<? extends Transaction<T>> transactions,
            Function<Transaction<T>, T> transactionPartToCheck, Function<T, CatalogType> toIdFunction, @Nullable Function<T, CatalogType> toIdFunction2,
        String descRoot, String permissionToCheck, Predicate<BlacklistNode> check) {
        if (target.hasPermission(bypass) || target.hasPermission(permissionToCheck)) {
            return false;
        }

        Map<String, BlacklistNode> blacklistNodeMap = itemDataService.getAllBlacklistedItems();

        // Transactions that are blacklisted.
        List<Transaction<T>> remove = Lists.newArrayList(transactions);

        remove.removeIf(x ->
            // Remove if both are not true.
            !check.test(blacklistNodeMap.getOrDefault(toIdFunction.apply(transactionPartToCheck.apply(x)).getId(), blacklistNode))
            && (toIdFunction2 == null || !check.test(blacklistNodeMap.getOrDefault(toIdFunction2.apply(transactionPartToCheck.apply(x)).getId(), blacklistNode))));
        if (remove.isEmpty()) {
            return false;
        }

        CatalogType id = toIdFunction.apply(transactionPartToCheck.apply(remove.get(0)));
        CatalogType id2 = toIdFunction2 == null ? null : toIdFunction2.apply(transactionPartToCheck.apply(remove.get(0)));

        String item;
        if (id2 == null || !(id2 instanceof Translatable) || id instanceof Translatable) {
            item = Util.getTranslatedStringFromItemId(id.getId()).orElse(Util.getTranslatableIfPresentOnCatalogType(id));
        } else {
            item = Util.getTranslatedStringFromItemId(id2.getId()).orElse(Util.getTranslatableIfPresentOnCatalogType(id2));
        }

        Optional<ItemStackSnapshot> replacement = bca.getNodeOrDefault().getItemStackIfShouldUseReplacement();
        Consumer<Transaction<T>> transaction = ItemStackSnapshot.class.isAssignableFrom(type) && replacement.isPresent()
            ? x -> x.setCustom((T)replacement.get()) : x -> x.setValid(false);

        remove.forEach(transaction);

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
        mp.put(use, new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.blacklist.bypassuse"), SuggestedLevel.ADMIN));
        mp.put(environment, new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.blacklist.bypassenvironment"), SuggestedLevel.ADMIN));
        mp.put(possess, new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.blacklist.bypasspossess"), SuggestedLevel.ADMIN));
        return mp;
    }

    private void checkHand(HandType type, Player player, ItemStack item) {
        if (itemDataService.getDataForItem(item.getItem().getId()).getBlacklist().isInventory()) {
            player.setItemInHand(type, null);
        }
    }
}
