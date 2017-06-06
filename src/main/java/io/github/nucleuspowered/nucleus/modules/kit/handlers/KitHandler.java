/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.handlers;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.events.NucleusKitEvent;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Kit;
import io.github.nucleuspowered.nucleus.api.service.NucleusKitService;
import io.github.nucleuspowered.nucleus.argumentparsers.KitArgument;
import io.github.nucleuspowered.nucleus.configurate.datatypes.KitDataNode;
import io.github.nucleuspowered.nucleus.dataservices.KitService;
import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import io.github.nucleuspowered.nucleus.modules.kit.KitModule;
import io.github.nucleuspowered.nucleus.modules.kit.commands.kit.KitCommand;
import io.github.nucleuspowered.nucleus.modules.kit.config.KitConfig;
import io.github.nucleuspowered.nucleus.modules.kit.config.KitConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.kit.datamodules.KitUserDataModule;
import io.github.nucleuspowered.nucleus.modules.kit.events.KitEvent;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.Tuple;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;

public class KitHandler implements NucleusKitService {

    private boolean isMustGetAll = false;
    private boolean isDropKitIfFull = false;
    private boolean isProcessTokens = false;

    private final CommandPermissionHandler cph = Nucleus.getNucleus()
            .getPermissionRegistry().getPermissionsForNucleusCommand(KitCommand.class);

    private final Map<Container, Tuple<KitArgument.KitInfo, Inventory>> inventoryKitMap = Maps.newHashMap();
    private final Map<Container, Tuple<KitArgument.KitInfo, Inventory>> inventoryKitCommandMap = Maps.newHashMap();
    private Tuple<Container, Inventory> firstJoinKitInventory = null;

    @Inject private KitService store;

    @Override
    public ImmutableMap<String, Kit> getKits() {
        return ImmutableMap.copyOf(store.getKits());
    }

    @Override
    public Set<String> getKitNames() {
        return ImmutableSet.copyOf(store.getKits().keySet());
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

    public synchronized void saveKit(KitArgument.KitInfo kitInfo) {
        saveKit(kitInfo.name, kitInfo.kit);
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

    public Optional<Tuple<KitArgument.KitInfo, Inventory>> getCurrentlyOpenInventoryCommandKit(Container inventory) {
        return Optional.ofNullable(inventoryKitCommandMap.get(inventory));
    }

    public boolean isCommandOpen(String kitName) {
        return inventoryKitCommandMap.values().stream().anyMatch(x -> x.getFirst().name.equalsIgnoreCase(kitName));
    }

    public void addKitCommandInventoryToListener(Tuple<KitArgument.KitInfo, Inventory> kit, Container inventory) {
        Preconditions.checkState(!inventoryKitCommandMap.containsKey(inventory));
        inventoryKitCommandMap.put(inventory, kit);
    }

    public void removeKitCommandInventoryFromListener(Container inventory) {
        inventoryKitCommandMap.remove(inventory);
    }

    public Optional<Tuple<Container, Inventory>> getFirstJoinKitInventory() {
        return Optional.ofNullable(firstJoinKitInventory);
    }

    public void setFirstJoinKitInventoryListener(@Nullable Tuple<Container, Inventory> firstJoinKitInventory) {
        this.firstJoinKitInventory = firstJoinKitInventory;
    }

    public boolean redeemKit(Kit kit, String kitName, Player player, CommandSource source, boolean performChecks)
        throws ReturnMessageException {
        return redeemKit(kit, kitName, player, source, performChecks, false);
    }

    public boolean redeemKit(Kit kit, String kitName, Player player, CommandSource source, boolean performChecks,
            boolean isFirstJoin) throws ReturnMessageException {

        KitUserDataModule user = Nucleus.getNucleus().getUserDataManager()
                .get(player.getUniqueId()).get().get(KitUserDataModule.class);
        MessageProvider messageProvider = Nucleus.getNucleus().getMessageProvider();
        Optional<Instant> oi = Util.getValueIgnoreCase(user.getKitLastUsedTime(), kitName);
        Instant now = Instant.now();
        if (performChecks) {

            // If the kit was used before...
            if (oi.isPresent()) {

                // if it's one time only and the user does not have an exemption...
                if (kit.isOneTime() && !player.hasPermission(cph.getPermissionWithSuffix("exempt.onetime"))) {
                    // tell the user.
                    if (player == source) {
                        throw ReturnMessageException.fromKey("command.kit.onetime.alreadyredeemed", kitName);
                    }

                    throw ReturnMessageException.fromKey("command.kit.give.onetime.alreadyredeemed",
                            Nucleus.getNucleus().getNameUtil().getSerialisedName(player), kitName);
                }

                // If we have a cooldown for the kit, and we don't have permission to
                // bypass it...
                if (!cph.testCooldownExempt(player) && kit.getInterval().getSeconds() > 0) {

                    // ...and we haven't reached the cooldown point yet...
                    Instant timeForNextUse = oi.get().plus(kit.getInterval());
                    if (timeForNextUse.isAfter(now)) {
                        Duration d = Duration.between(now, timeForNextUse);

                        // tell the user.
                        if (player == source) {
                            throw ReturnMessageException.fromKey("command.kit.cooldown", Util.getTimeStringFromSeconds(d.getSeconds()), kitName);
                        }

                        throw ReturnMessageException.fromKey("command.kit.give.cooldown",
                            Nucleus.getNucleus().getNameUtil().getSerialisedName(player), Util.getTimeStringFromSeconds(d.getSeconds()), kitName);
                    }
                }
            }
        }

        // Kit pre redeem
        Cause cause = Cause.of(NamedCause.owner(player));
        NucleusKitEvent.Redeem.Pre preEvent = new KitEvent.PreRedeem(cause, oi.orElse(null), kitName, kit, player);
        if (Sponge.getEventManager().post(preEvent)) {
            throw new ReturnMessageException(preEvent.getCancelMessage()
                    .orElseGet(() -> (Nucleus.getNucleus()
                            .getMessageProvider().getTextMessageWithFormat("command.kit.cancelledpre", kitName))));
        }


        List<Optional<ItemStackSnapshot>> slotList = Lists.newArrayList();
        Util.getStandardInventory(player).slots().forEach(x -> slotList.add(x.peek().map(ItemStack::createSnapshot)));

        Tristate tristate = Util.addToStandardInventory(player, kit.getStacks(),
                !isFirstJoin && !isMustGetAll && isDropKitIfFull, isProcessTokens);
        if (!isFirstJoin && tristate != Tristate.TRUE) {
            if (isMustGetAll) {
                Inventory inventory = Util.getStandardInventory(player);

                // Slots
                Iterator<Inventory> slot = inventory.slots().iterator();

                // Slots to restore
                slotList.forEach(x -> {
                    Inventory i = slot.next();
                    i.clear();
                    x.ifPresent(y -> i.offer(y.createStack()));
                });

                if (player == source) {
                    throw ReturnMessageException.fromKey("command.kit.fullinventorynosave", kitName);
                } else {
                    throw ReturnMessageException.fromKey("command.kit.give.fullinventorynosave",
                            Nucleus.getNucleus().getNameUtil().getSerialisedName(player));
                }
            }

            if (isDropKitIfFull) {
                if (player == source) {
                    source.sendMessage(messageProvider.getTextMessageWithFormat("command.kit.itemsdropped"));
                } else {
                    source.sendMessage(messageProvider.getTextMessageWithFormat("command.kit.give.itemsdropped",
                            Nucleus.getNucleus().getNameUtil().getSerialisedName(player)));
                }
            } else {
                if (player == source) {
                    source.sendMessage(messageProvider.getTextMessageWithFormat("command.kit.fullinventory"));
                } else {
                    source.sendMessage(messageProvider.getTextMessageWithFormat("command.kit.give.fullinventory",
                        Nucleus.getNucleus().getNameUtil().getSerialisedName(player)));
                }
            }
        }

        // If something was consumed, consider a success.
        if (isDropKitIfFull || tristate != Tristate.FALSE) {
            kit.redeemKitCommands(player);
            if (player != source) {
                source.sendMessage(
                    messageProvider.getTextMessageWithTextFormat("command.kit.give.spawned",
                            Nucleus.getNucleus().getNameUtil().getName(player), Text.of(kitName)));
            }

            // Register the last used time. Do it for everyone, in case
            // permissions or cooldowns change later
            if (performChecks) {
                user.addKitLastUsedTime(kitName, now);
            }

            Sponge.getEventManager().post(new KitEvent.PostRedeem(cause, oi.orElse(null), kitName, kit, player));
            if (!isFirstJoin && kit.isDisplayMessageOnRedeem()) {
                player.sendMessage(messageProvider.getTextMessageWithFormat("command.kit.spawned", kitName));
            }

            return true;
        } else {
            // Failed.
            Sponge.getEventManager().post(new KitEvent.FailedRedeem(cause, oi.orElse(null), kitName, kit, player));
            if (player == source) {
                throw ReturnMessageException.fromKey("command.kit.fail", kitName);
            } else {
                throw ReturnMessageException.fromKeyText("command.kit.give.fail",
                        Nucleus.getNucleus().getNameUtil().getName(player), Text.of(kitName));
            }
        }
    }

    public void reload() {
        Nucleus.getNucleus()
            .getConfigAdapter(KitModule.ID, KitConfigAdapter.class)
            .ifPresent(x -> {
                KitConfig k = x.getNodeOrDefault();
                this.isDropKitIfFull = k.isDropKitIfFull();
                this.isMustGetAll = k.isMustGetAll();
                this.isProcessTokens = k.isProcessTokens();
            });
    }
}
