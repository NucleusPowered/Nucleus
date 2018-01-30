/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.handlers;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.events.NucleusKitEvent;
import io.github.nucleuspowered.nucleus.api.exceptions.KitRedeemException;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Kit;
import io.github.nucleuspowered.nucleus.api.service.NucleusKitService;
import io.github.nucleuspowered.nucleus.dataservices.KitService;
import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.text.NucleusTextTemplateFactory;
import io.github.nucleuspowered.nucleus.modules.kit.KitModule;
import io.github.nucleuspowered.nucleus.modules.kit.commands.kit.KitCommand;
import io.github.nucleuspowered.nucleus.modules.kit.config.KitConfig;
import io.github.nucleuspowered.nucleus.modules.kit.config.KitConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.kit.datamodules.KitUserDataModule;
import io.github.nucleuspowered.nucleus.modules.kit.events.KitEvent;
import io.github.nucleuspowered.nucleus.util.CauseStackHelper;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.Tuple;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class KitHandler implements NucleusKitService {

    private static final InventoryTransactionResult EMPTY_ITR =
            InventoryTransactionResult.builder().type(InventoryTransactionResult.Type.SUCCESS).build();

    private static final Pattern inventory = Pattern.compile("\\{\\{.+?}}");

    public static String getPermissionForKit(String kitName) {
        return PermissionRegistry.PERMISSIONS_PREFIX + "kits." + kitName.toLowerCase();
    }

    private boolean isProcessTokens = false;
    private boolean isMustGetAll = false;

    private final CommandPermissionHandler cph = Nucleus.getNucleus()
            .getPermissionRegistry().getPermissionsForNucleusCommand(KitCommand.class);

    private final List<Container> viewers = Lists.newArrayList();
    private final Map<Container, Tuple<Kit, Inventory>> inventoryKitMap = Maps.newHashMap();
    private final Map<Container, Tuple<Kit, Inventory>> inventoryKitCommandMap = Maps.newHashMap();

    private final KitService store = Nucleus.getNucleus().getKitService();

    @Override
    public Set<String> getKitNames() {
        return getKitNames(true);
    }

    public Set<String> getKitNames(boolean showHidden) {
        return this.store.getKitNames(showHidden);
    }

    @Override
    public Optional<Kit> getKit(String name) {
        return this.store.getKit(name).map(x -> new SingleKit(name, x));
    }

    @Override
    public Collection<ItemStack> getItemsForPlayer(Kit kit, Player player) {
        Collection<ItemStack> cis = kit.getStacks().stream().map(ItemStackSnapshot::createStack).collect(Collectors.toList());
        if (this.isProcessTokens) {
            processTokensInItemStacks(player, cis);
        }

        return cis;
    }

    @Override
    public RedeemResult redeemKit(Kit kit, Player player, boolean performChecks) throws KitRedeemException {
        return redeemKit(kit, player, performChecks, this.isMustGetAll, false);
    }

    @Override
    public RedeemResult redeemKit(Kit kit, Player player, boolean performChecks, boolean mustRedeemAll) throws KitRedeemException {
        return redeemKit(kit, player, performChecks, mustRedeemAll, false);
    }

    public RedeemResult redeemKit(Kit kit, Player player, boolean performChecks, boolean isMustGetAll, boolean isFirstJoin) throws
            KitRedeemException {
        KitUserDataModule user = Nucleus.getNucleus().getUserDataManager().get(player.getUniqueId()).get().get(KitUserDataModule.class);
        Optional<Instant> oi = Util.getValueIgnoreCase(user.getKitLastUsedTime(), kit.getName());
        Instant now = Instant.now();
        if (performChecks) {

            // If the kit was used before...
            if (oi.isPresent()) {

                // if it's one time only and the user does not have an exemption...
                if (kit.isOneTime() && !player.hasPermission(cph.getPermissionWithSuffix("exempt.onetime"))) {
                    throw new KitRedeemException("Already redeemed", KitRedeemException.Reason.ALREADY_REDEEMED);
                }

                // If we have a cooldown for the kit, and we don't have permission to
                // bypass it...
                if (!cph.testCooldownExempt(player) && kit.getCooldown().map(Duration::getSeconds).orElse(0L) > 0) {

                    // ...and we haven't reached the cooldown point yet...
                    Instant timeForNextUse = oi.get().plus(kit.getCooldown().get());
                    if (timeForNextUse.isAfter(now)) {
                        throw new KitRedeemException.Cooldown("Cooldown not expired", Duration.between(now, timeForNextUse));
                    }
                }
            }
        }

        // Kit pre redeem
        Cause cause = CauseStackHelper.createCause(player);

        NucleusKitEvent.Redeem.Pre preEvent = new KitEvent.PreRedeem(cause, oi.orElse(null), kit, player);
        if (Sponge.getEventManager().post(preEvent)) {
            throw new KitRedeemException.PreCancelled(preEvent.getCancelMessage().orElse(null));
        }

        List<Optional<ItemStackSnapshot>> slotList = Lists.newArrayList();
        Util.getStandardInventory(player).slots().forEach(x -> slotList.add(x.peek().map(ItemStack::createSnapshot)));

        InventoryTransactionResult inventoryTransactionResult = EMPTY_ITR;
        if (!kit.getStacks().isEmpty()) {
            inventoryTransactionResult = addToStandardInventory(player, kit.getStacks(), isProcessTokens);
            if (!isFirstJoin && !inventoryTransactionResult.getRejectedItems().isEmpty() && isMustGetAll) {
                Inventory inventory = Util.getStandardInventory(player);

                // Slots
                Iterator<Inventory> slot = inventory.slots().iterator();

                // Slots to restore
                slotList.forEach(x -> {
                    Inventory i = slot.next();
                    i.clear();
                    x.ifPresent(y -> i.offer(y.createStack()));
                });

                // My friend was playing No Man's Sky, I almost wrote "No free slots in suit inventory".
                throw new KitRedeemException("No free slots in player inventory", KitRedeemException.Reason.NO_SPACE);
            }
        }

        // If something was consumed, consider a success.
        if (inventoryTransactionResult.getType() == InventoryTransactionResult.Type.SUCCESS) {
            kit.redeemKitCommands(player);

            // Register the last used time. Do it for everyone, in case
            // permissions or cooldowns change later
            if (performChecks) {
                user.addKitLastUsedTime(kit.getName(), now);
            }

            Sponge.getEventManager().post(new KitEvent.PostRedeem(cause, oi.orElse(null), kit, player));
            return new KitRedeemResult(inventoryTransactionResult.getRejectedItems(), slotList.stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList()));
        } else {
            // Failed.
            Sponge.getEventManager().post(new KitEvent.FailedRedeem(cause, oi.orElse(null), kit, player));
            throw new KitRedeemException("No items were redeemed", KitRedeemException.Reason.UNKNOWN);
        }
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
    public synchronized void saveKit(Kit kit) {
        Preconditions.checkArgument(kit instanceof SingleKit);
        Util.getKeyIgnoreCase(store.getKitNames(true), kit.getName()).ifPresent(store::removeKit);
        store.addKit(kit);
        store.save();
    }

    @Override
    public Kit createKit(String name) throws IllegalArgumentException {
        Optional<String> key = Util.getKeyIgnoreCase(store.getKitNames(true), name);
        key.ifPresent(s -> {
            throw new IllegalArgumentException("Kit " + name + " already exists!");
        });
        return new SingleKit(name);
    }

    public Optional<Tuple<Kit, Inventory>> getCurrentlyOpenInventoryKit(Container inventory) {
        return Optional.ofNullable(inventoryKitMap.get(inventory));
    }

    public boolean isOpen(String kitName) {
        return inventoryKitMap.values().stream().anyMatch(x -> x.getFirst().getName().equalsIgnoreCase(kitName));
    }

    public void addKitInventoryToListener(Tuple<Kit, Inventory> kit, Container inventory) {
        Preconditions.checkState(!inventoryKitMap.containsKey(inventory));
        inventoryKitMap.put(inventory, kit);
    }

    public void removeKitInventoryFromListener(Container inventory) {
        inventoryKitMap.remove(inventory);
    }

    public Optional<Tuple<Kit, Inventory>> getCurrentlyOpenInventoryCommandKit(Container inventory) {
        return Optional.ofNullable(inventoryKitCommandMap.get(inventory));
    }

    public void addKitCommandInventoryToListener(Tuple<Kit, Inventory> kit, Container inventory) {
        Preconditions.checkState(!inventoryKitCommandMap.containsKey(inventory));
        inventoryKitCommandMap.put(inventory, kit);
    }

    public void removeKitCommandInventoryFromListener(Container inventory) {
        inventoryKitCommandMap.remove(inventory);
    }

    public void addViewer(Container inventory) {
        this.viewers.add(inventory);
    }

    public void removeViewer(Container inventory) {
        this.viewers.remove(inventory);
        this.viewers.removeIf(x -> !x.hasViewers());
    }

    public boolean isViewer(Container inventory) {
        return this.viewers.contains(inventory);
    }

    public void reload() {
        KitConfig kc = Nucleus.getNucleus().getConfigValue(
                KitModule.ID,
                KitConfigAdapter.class,
                x -> x).orElse(new KitConfig());
        this.isProcessTokens = kc.isProcessTokens();
        this.isMustGetAll = kc.isMustGetAll();
    }

    public void processTokensInItemStacks(Player player, Collection<ItemStack> stacks) {
        final Matcher m = inventory.matcher("");
        for (ItemStack x : stacks) {
            x.get(Keys.DISPLAY_NAME).ifPresent(text -> {
                if (m.reset(text.toPlain()).find()) {
                    x.offer(Keys.DISPLAY_NAME,
                            NucleusTextTemplateFactory.createFromAmpersandString(TextSerializers.FORMATTING_CODE.serialize(text))
                                    .getForCommandSource(player, null, null));
                }
            });

            x.get(Keys.ITEM_LORE).ifPresent(text -> {
                if (text.stream().map(Text::toPlain).anyMatch(y -> m.reset(y).find())) {
                    x.offer(Keys.ITEM_LORE,
                            text.stream().map(y ->
                                    NucleusTextTemplateFactory.createFromAmpersandString(TextSerializers.FORMATTING_CODE.serialize(y))
                                            .getForCommandSource(player, null, null)).collect(Collectors.toList()));
                }
            });
        }
    }

    /**
     * Adds items to a {@link Player}s {@link Inventory}
     * @param player The {@link Player}
     * @param itemStacks The {@link ItemStackSnapshot}s to add.
     * @param replaceTokensInLore If true, the display name
     * @return {@link Tristate#TRUE} if everything is successful, {@link Tristate#FALSE} if nothing was added, {@link Tristate#UNDEFINED}
     * if some stacks were added.
     */
    private InventoryTransactionResult addToStandardInventory(
            Player player, Collection<ItemStackSnapshot> itemStacks, final boolean replaceTokensInLore) {

        Inventory target = Util.getStandardInventory(player);
        InventoryTransactionResult.Builder resultBuilder = InventoryTransactionResult.builder();

        Collection<ItemStack> toOffer = itemStacks.stream()
                .filter(x -> x.getType() != ItemTypes.NONE)
                .map(ItemStackSnapshot::createStack)
                .collect(Collectors.toList());

        if (replaceTokensInLore) {
            processTokensInItemStacks(player, toOffer);
        }

        boolean success = false;
        for (ItemStack stack : toOffer) {
            InventoryTransactionResult itr = target.offer(stack);
            success = success || itr.getType() == InventoryTransactionResult.Type.SUCCESS;
            for (ItemStackSnapshot iss : itr.getRejectedItems()) {
                resultBuilder.reject(iss.createStack());
            }

        }

        return resultBuilder.type(success ? InventoryTransactionResult.Type.SUCCESS : InventoryTransactionResult.Type.FAILURE).build();
    }
}
