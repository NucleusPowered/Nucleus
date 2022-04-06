/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.services;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.api.module.kit.KitRedeemResult;
import io.github.nucleuspowered.nucleus.api.module.kit.NucleusKitService;
import io.github.nucleuspowered.nucleus.api.module.kit.data.Kit;
import io.github.nucleuspowered.nucleus.api.module.kit.event.NucleusKitEvent;
import io.github.nucleuspowered.nucleus.modules.kit.KitKeys;
import io.github.nucleuspowered.nucleus.modules.kit.KitPermissions;
import io.github.nucleuspowered.nucleus.modules.kit.config.KitConfig;
import io.github.nucleuspowered.nucleus.modules.kit.events.KitEvent;
import io.github.nucleuspowered.nucleus.modules.kit.misc.KitRedeemResultImpl;
import io.github.nucleuspowered.nucleus.modules.kit.data.SingleKit;
import io.github.nucleuspowered.nucleus.modules.kit.parameters.KitParameter;
import io.github.nucleuspowered.nucleus.modules.kit.storage.IKitDataObject;
import io.github.nucleuspowered.nucleus.modules.kit.storage.KitStorageModule;
import io.github.nucleuspowered.nucleus.core.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.core.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.INucleusTextTemplateFactory;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IStorageManager;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.services.IStorageService;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.util.Tristate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@APIService(NucleusKitService.class)
public class KitService implements NucleusKitService, IReloadableService.Reloadable, ServiceBase {

    public static final Parameter.Key<Kit> KIT_KEY = Parameter.key("kit", Kit.class);

    private static final InventoryTransactionResult EMPTY_ITR =
            InventoryTransactionResult.builder().type(InventoryTransactionResult.Type.SUCCESS).build();

    private static final Pattern inventory = Pattern.compile("\\{\\{.+?}}");
    private final IStorageManager storageManager;
    private final IPermissionService permissionService;
    private final IMessageProviderService messageProviderService;
    private final INucleusTextTemplateFactory textTemplateFactory;
    private final Logger logger;

    private boolean isProcessTokens = false;
    private boolean isMustGetAll = false;

    private final Parameter.Value<Kit> withPermission;
    private final Parameter.Value<Kit> withoutPermission;

    @Inject
    public KitService(final INucleusServiceCollection serviceCollection) {
        this.permissionService = serviceCollection.permissionService();
        this.storageManager = serviceCollection.storageManager();
        this.messageProviderService = serviceCollection.messageProvider();
        this.textTemplateFactory = serviceCollection.textTemplateFactory();
        this.logger = serviceCollection.logger();

        this.withPermission = Parameter.builder(Kit.class)
                .addParser(new KitParameter(serviceCollection, this, true))
                .key(KitService.KIT_KEY)
                .build();
        this.withoutPermission = Parameter.builder(Kit.class)
                .addParser(new KitParameter(serviceCollection, this, false))
                .key(KitService.KIT_KEY)
                .build();
    }

    public Parameter.Value<Kit> kitParameterWithPermission() {
        return this.withPermission;
    }

    public Parameter.Value<Kit> kitParameterWithoutPermission() {
        return this.withoutPermission;
    }

    public boolean exists(final String name, final boolean includeHidden) {
        return this.getKitNames(includeHidden).stream().anyMatch(x -> x.equalsIgnoreCase(name));
    }

    @Override
    public Set<String> getKitNames() {
        return this.getKitNames(true);
    }

    @Override
    public Optional<Kit> getKit(final String name) {
        return Optional.ofNullable(this.getKits().getKitMap().get(name.toLowerCase()));
    }

    @Override
    public Collection<ItemStack> getItemsForPlayer(final Kit kit, final UUID uuid) {
        final ServerPlayer serverPlayer = this.player(uuid);
        final Collection<ItemStack> cis = kit.getStacks().stream().map(ItemStackSnapshot::createStack).collect(Collectors.toList());
        if (this.isProcessTokens) {
            this.processTokensInItemStacks(serverPlayer, cis);
        }

        return cis;
    }

    private ServerPlayer player(final UUID uuid) {
        return Sponge.server().player(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Player with supplied UUID is not online"));
    }

    @Override
    public CompletableFuture<Boolean> hasPreviouslyRedeemed(final Kit kit, final UUID user) {
        return this.redeemTime(kit.getName(), user).thenApply(Optional::isPresent);
    }

    @Override
    public CompletableFuture<Boolean> isRedeemable(final Kit kit, final UUID user) {
        return this.redeemTime(kit.getName(), user)
                .thenApply(x -> {
                            if (x.isPresent()) {
                                if (kit.isOneTime()) {
                                    return false;
                                }

                                return !this.getNextUseTime(kit, user, x.get()).isPresent();
                            }
                            return true;
                        }
                );
    }

    @Override
    public CompletableFuture<Optional<Instant>> getCooldownExpiry(final Kit kit, final UUID user) {
        return this.redeemTime(kit.getName(), user).thenApply(x -> {
            if (x.isPresent() && kit.getCooldown().isPresent()) {
                final Instant adjustedInstant = x.get().plus(kit.getCooldown().get());
                if (adjustedInstant.isAfter(Instant.now())) {
                    return Optional.of(adjustedInstant);
                }
            }
            return Optional.empty();
        });
    }

    private CompletableFuture<Optional<Instant>> redeemTime(final String name, final UUID user) {
        return this.getUserRedemptionData(user).thenApply(x -> Optional.ofNullable(x.get(name)));
    }

    @Override
    public KitRedeemResult redeemKit(final Kit kit, final UUID player, final boolean performChecks) {
        return this.redeemKit(kit, this.player(player), performChecks, performChecks, this.isMustGetAll, false);
    }

    @Override
    public KitRedeemResult redeemKit(final Kit kit, final UUID player, final boolean performChecks, final boolean mustRedeemAll) {
        return this.redeemKit(kit, this.player(player), performChecks, performChecks, mustRedeemAll, false);
    }

    public KitRedeemResult redeemKit(final Kit kit,
            final ServerPlayer player,
            final boolean checkOneTime,
            final boolean checkCooldown,
            final boolean isMustGetAll,
            final boolean isFirstJoin) {
        final UUID playerUUID = player.uniqueId();
        KitRedeemResult result = null;

        final Map<String, Instant> redeemed = this.getUserRedemptionData(player.uniqueId()).join();

        final Instant timeOfLastUse = redeemed.get(kit.getName().toLowerCase());
        final Instant now = Instant.now();

        try (final CauseStackManager.StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame()) {
            frame.pushCause(player);

            // If the kit was used before...
            // Get original list
            final Collection<ItemStackSnapshot> original = this.getItems(kit, this.isProcessTokens, player);
            final Collection<String> commands = kit.getCommands();
            final Optional<Instant> instant = this.getNextUseTime(kit, playerUUID, timeOfLastUse);
            if ((checkOneTime || checkCooldown) && timeOfLastUse != null) {

                // if it's one time only and the user does not have an exemption...
                if (checkOneTime && !this.checkOneTime(kit, playerUUID)) {
                    Sponge.eventManager().post(
                            new KitEvent.FailedRedeem(frame.currentCause(), timeOfLastUse, kit, player,
                                    original, null, commands, null, KitRedeemResult.Status.ALREADY_REDEEMED_ONE_TIME));
                    result = new KitRedeemResultImpl(
                            KitRedeemResult.Status.ALREADY_REDEEMED_ONE_TIME,
                            Collections.emptyList(),
                            null,
                            null
                    );
                } else if (checkCooldown) {
                    if (instant.isPresent()) {
                        Sponge.eventManager().post(
                                new KitEvent.FailedRedeem(frame.currentCause(), timeOfLastUse, kit, player,
                                        original, null, commands, null, KitRedeemResult.Status.COOLDOWN_NOT_EXPIRED));
                        result = new KitRedeemResultImpl(
                                KitRedeemResult.Status.COOLDOWN_NOT_EXPIRED,
                                Collections.emptyList(),
                                instant.get(),
                                null
                        );
                    }
                }

            }
            if (result == null) {
                final NucleusKitEvent.Redeem.Pre preEvent =
                        new KitEvent.PreRedeem(frame.currentCause(), timeOfLastUse, kit, player, original, commands);
                if (Sponge.eventManager().post(preEvent)) {
                    Sponge.eventManager().post(
                            new KitEvent.FailedRedeem(frame.currentCause(), timeOfLastUse, kit, player, original,
                                    preEvent.getStacksToRedeem().orElse(null),
                                    commands,
                                    preEvent.getCommandsToExecute().orElse(null),
                                    KitRedeemResult.Status.PRE_EVENT_CANCELLED));
                    result = new KitRedeemResultImpl(
                            KitRedeemResult.Status.PRE_EVENT_CANCELLED,
                            Collections.emptyList(),
                            instant.orElse(null),
                            preEvent.getCancelMessage().orElse(null)
                    );
                } else {
                    final List<ItemStackSnapshot> slotList = new ArrayList<>();
                    for (final Slot slot : Util.getStandardInventory(player).slots()) {
                        slotList.add(slot.peek().createSnapshot());
                    }
                    InventoryTransactionResult inventoryTransactionResult = EMPTY_ITR;
                    KitRedeemResultImpl ex = null;
                    if (!kit.getStacks().isEmpty()) {
                        inventoryTransactionResult =
                                this.addToStandardInventory(player, preEvent.getStacksToRedeem().orElseGet(preEvent::getOriginalStacksToRedeem));
                        if (!isFirstJoin && !inventoryTransactionResult.rejectedItems().isEmpty() && isMustGetAll) {
                            final Inventory inventory = Util.getStandardInventory(player);

                            // Slots
                            final Iterator<Slot> slot = inventory.slots().iterator();

                            // Slots to restore
                            slotList.forEach(x -> {
                                final Inventory i = slot.next();
                                i.clear();
                                if (!x.isEmpty()) {
                                    i.offer(x.createStack());
                                }
                            });

                            ex = new KitRedeemResultImpl(
                                    KitRedeemResult.Status.NO_SPACE,
                                    inventoryTransactionResult.rejectedItems(),
                                    instant.orElse(null),
                                    preEvent.getCancelMessage().orElse(null));
                        }
                    }// If something was consumed, consider a success.
                    if (ex == null && inventoryTransactionResult.type() == InventoryTransactionResult.Type.SUCCESS) {
                        this.redeemKitCommands(preEvent.getCommandsToExecute().orElse(commands), player);

                        // Register the last used time. Do it for everyone, in case
                        // permissions or cooldowns change later
                        if (checkCooldown) {
                            redeemed.put(kit.getName().toLowerCase(), now);
                            this.setUserRedemptionData(playerUUID, redeemed);
                        }

                        Sponge.eventManager().post(new KitEvent.PostRedeem(frame.currentCause(), timeOfLastUse, kit, player, original,
                                preEvent.getStacksToRedeem().orElse(null),
                                commands,
                                preEvent.getCommandsToExecute().orElse(null)));

                        final Optional<Instant> nextCooldown = this.getNextUseTime(kit, playerUUID, Instant.now());

                        result = new KitRedeemResultImpl(
                                inventoryTransactionResult.rejectedItems().isEmpty() ?
                                        KitRedeemResult.Status.SUCCESS : KitRedeemResult.Status.PARTIAL_SUCCESS,
                                inventoryTransactionResult.rejectedItems(),
                                nextCooldown.orElse(null),
                                null);
                    } else {
                        // Failed.
                        ex = ex == null ? new KitRedeemResultImpl(
                                KitRedeemResult.Status.UNKNOWN,
                                inventoryTransactionResult.rejectedItems(),
                                instant.orElse(null),
                                null) : ex;
                        Sponge.eventManager().post(new KitEvent.FailedRedeem(frame.currentCause(), timeOfLastUse, kit, player, original,
                                preEvent.getStacksToRedeem().orElse(null),
                                commands,
                                preEvent.getCommandsToExecute().orElse(null),
                                ex.getStatus()));
                        result = ex;
                    }
                }

            }

        }
        return result;
    }

    private boolean redeemKitCommands(final Collection<String> commands, final ServerPlayer player) {
        final String playerName = player.name();
        boolean success = true;
        try (final CauseStackManager.StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame()) {
            frame.pushCause(Sponge.systemSubject());
            for (final String command : commands) {
                try {
                    Sponge.server().commandManager().process(command.replace("{{player}}", playerName));
                } catch (final CommandException e) {
                    e.printStackTrace();
                    success = false;
                }
            }
        }
        return success;
    }

    public boolean checkOneTime(final Kit kit, final UUID player) {
        // if it's one time only and the user does not have an exemption...
        return !kit.isOneTime() || this.permissionService.hasPermission(player, KitPermissions.KIT_EXEMPT_ONETIME);
    }

    public Optional<Instant> getNextUseTime(final Kit kit, final UUID player, final Instant timeOfLastUse) {
        // If the kit was used before...
        if (timeOfLastUse != null) {

            // If we have a cooldown for the kit, and we don't have permission to
            // bypass it...
            if (!this.permissionService.hasPermission(player, KitPermissions.KIT_EXEMPT_COOLDOWN)
                    && kit.getCooldown().map(Duration::getSeconds).orElse(0L) > 0) {

                // ...and we haven't reached the cooldown point yet...
                final Instant timeForNextUse = timeOfLastUse.plus(kit.getCooldown().get());
                if (timeForNextUse.isAfter(Instant.now())) {
                    return Optional.of(timeForNextUse);
                }
            }
        }

        return Optional.empty();
    }

    private CompletableFuture<Map<String, Instant>> getUserRedemptionData(final UUID user) {
        return this.storageManager
                .getUserService()
                .getOrNew(user)
                .thenApply(
                        dataObject -> dataObject
                                .get(KitKeys.REDEEMED_KITS)
                                .orElseGet(HashMap::new)
                );
    }

    private void setUserRedemptionData(final UUID user, final Map<String, Instant> set) {
        this.storageManager.getUserService().setAndSave(user, KitKeys.REDEEMED_KITS, set);
    }

    // ---

    @Override
    public void saveKit(final Kit kit) {
        this.saveKit(kit, true);
    }

    public void saveKit(final Kit kit, final boolean save) {
        final IStorageService.Single<IKitDataObject> kdo = this.getKitService();
        final IKitDataObject kitDataObject = kdo.getOrNewOnThread();
        final Map<String, Kit> kits = new HashMap<>(kitDataObject.getKitMap());
        Util.getKeyIgnoreCase(this.getKitNames(true), kit.getName()).ifPresent(kits::remove);
        kits.put(kit.getName().toLowerCase(), kit);
        try {
            kitDataObject.setKitMap(kits);
            if (save) {
                kdo.save(kitDataObject);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Kit createKit(final String name) throws IllegalArgumentException {
        final IKitDataObject kitDataObject = this.getKits();
        final Map<String, Kit> kits = new HashMap<>(kitDataObject.getKitMap());
        Util.getKeyIgnoreCase(kits, name).ifPresent(s -> {
            throw new IllegalArgumentException("Kit " + name + " already exists!");
        });
        final Kit kit = new SingleKit(name);
        this.saveKit(kit, true);
        return kit;
    }

    @Override
    public void renameKit(final String kitName, final String newKitName) throws IllegalArgumentException {
        final String from = kitName.toLowerCase();
        final String to = newKitName.toLowerCase();
        final Kit targetKit = this.getKit(from).orElseThrow(() -> new IllegalArgumentException(
                this.messageProviderService.getMessageString("kit.noexists", kitName)));
        if (this.getKit(to).isPresent()) {
            throw new IllegalArgumentException(this.messageProviderService.getMessageString("kit.cannotrename", from, to));
        }
        this.saveKit(new SingleKit(kitName.toLowerCase(), targetKit), true);
        this.removeKit(from);
    }

    public void processTokensInItemStacks(final Player player, final Collection<ItemStack> stacks) {
        final Matcher m = inventory.matcher("");
        for (final ItemStack x : stacks) {
            x.get(Keys.CUSTOM_NAME).ifPresent(text -> {
                if (m.reset(PlainComponentSerializer.plain().serialize(text)).find()) {
                    x.offer(Keys.CUSTOM_NAME,
                            this.textTemplateFactory
                                    .createFromAmpersandString(LegacyComponentSerializer.legacyAmpersand().serialize(text))
                                    .getForObject(player));
                }
            });

            x.get(Keys.LORE).ifPresent(text -> {
                if (text.stream().map(PlainComponentSerializer.plain()::serialize).anyMatch(y -> m.reset(y).find())) {
                    x.offer(Keys.LORE,
                            text.stream().map(y ->
                                    this.textTemplateFactory
                                            .createFromAmpersandString(LegacyComponentSerializer.legacyAmpersand().serialize(y))
                                            .getForObject(player)).collect(Collectors.toList()));
                }
            });
        }
    }

    private Collection<ItemStackSnapshot> getItems(final Kit kit, final boolean replaceTokensInLore, final Player targetPlayer) {
        final Collection<ItemStack> toOffer = kit.getStacks().stream()
                .filter(x -> !x.isEmpty())
                .map(ItemStackSnapshot::createStack)
                .collect(Collectors.toList());

        if (replaceTokensInLore) {
            this.processTokensInItemStacks(targetPlayer, toOffer);
        }

        return toOffer.stream().map(ItemStack::createSnapshot).collect(Collectors.toList());
    }

    /**
     * Adds items to a {@link Player}s {@link Inventory}
     * @param player The {@link Player}
     * @param itemStacks The {@link ItemStackSnapshot}s to add.
     * @return {@link Tristate#TRUE} if everything is successful, {@link Tristate#FALSE} if nothing was added, {@link Tristate#UNDEFINED}
     * if some stacks were added.
     */
    private InventoryTransactionResult addToStandardInventory(
            final Player player, final Collection<ItemStackSnapshot> itemStacks) {

        final Inventory target = Util.getStandardInventory(player);
        final InventoryTransactionResult.Builder resultBuilder = InventoryTransactionResult.builder();

        final Collection<ItemStack> toOffer = itemStacks.stream()
                .filter(x -> !x.isEmpty())
                .map(ItemStackSnapshot::createStack)
                .collect(Collectors.toList());

        boolean success = false;
        for (final ItemStack stack : toOffer) {
            final InventoryTransactionResult itr = target.offer(stack);
            success = success || itr.type() == InventoryTransactionResult.Type.SUCCESS;
            for (final ItemStackSnapshot iss : itr.rejectedItems()) {
                resultBuilder.reject(iss.createStack());
            }

        }

        return resultBuilder.type(success ? InventoryTransactionResult.Type.SUCCESS : InventoryTransactionResult.Type.FAILURE).build();
    }

    // --

    public Set<String> getKitNames(final boolean showHidden) {
        return this.getKits().getKitMap().entrySet().stream()
                .filter(x -> showHidden || (!x.getValue().isHiddenFromList() && !x.getValue().isFirstJoinKit()))
                .map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    public List<Kit> getFirstJoinKits() {
        return this.getKits().getKitMap().values()
                .stream()
                .filter(Kit::isFirstJoinKit)
                .collect(Collectors.toList());
    }

    public List<Kit> getAutoRedeemable() {
        return this.getKits().getKitMap()
                .values()
                .stream()
                .filter(x -> x.isAutoRedeem() && x.getCost() <= 0)
                .collect(Collectors.toList());
    }

    public boolean removeKit(final String name) {
        boolean r = false;
        try {
            r = this.getKits().removeKit(name.toLowerCase());
        } catch (final Exception e) {
            this.logger.error("Could not update kits", e);
        }
        return r;
    }

    // --

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        final KitConfig kitConfig = serviceCollection.configProvider().getModuleConfig(KitConfig.class);
        this.isMustGetAll = kitConfig.isMustGetAll();
        this.isProcessTokens = kitConfig.isProcessTokens();
    }

    public IStorageService.SingleCached<IKitDataObject> getKitService() {
        return this.storageManager.getAdditionalStorageServiceForDataObject(KitStorageModule.class).get();
    }

    private IKitDataObject getKits() {
        return this.getKitService().getOrNewOnThread();
    }

}
