/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.commands;

import io.github.nucleuspowered.nucleus.modules.item.ItemPermissions;
import io.github.nucleuspowered.nucleus.modules.item.config.ItemConfig;
import io.github.nucleuspowered.nucleus.modules.item.config.RepairConfig;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.registry.RegistryEntry;
import org.spongepowered.api.registry.RegistryTypes;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@EssentialsEquivalent({"repair", "fix"})
@Command(
        aliases = { "repair", "mend", "fix" },
        basePermission = ItemPermissions.BASE_REPAIR,
        commandDescriptionKey = "repair",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = ItemPermissions.EXEMPT_COOLDOWN_REPAIR),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = ItemPermissions.EXEMPT_WARMUP_REPAIR),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = ItemPermissions.EXEMPT_COST_REPAIR)
        },
        associatedPermissions = ItemPermissions.OTHERS_REPAIR
)
public class RepairCommand implements ICommandExecutor, IReloadableService.Reloadable {

    private boolean whitelist = false;
    private List<ItemType> restrictions = new ArrayList<>();

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        final RepairConfig config = serviceCollection.configProvider().getModuleConfig(ItemConfig.class).getRepairConfig();
        this.whitelist = config.isWhitelist();
        this.restrictions = config.getRestrictions().stream()
                .map(x -> Sponge.game().registry(RegistryTypes.ITEM_TYPE).findValue(x).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Flag[] flags(final INucleusServiceCollection serviceCollection) {
        final IPermissionService permissionService = serviceCollection.permissionService();
        return new Flag[] {
                Flag.of("m", "mainhand"),
                Flag.builder().setRequirement(x -> permissionService.hasPermission(x, ItemPermissions.REPAIR_FLAG_ALL)).alias("a").alias("all").build(),
                Flag.builder().setRequirement(x -> permissionService.hasPermission(x, ItemPermissions.REPAIR_FLAG_HOTBAR)).alias("h").alias("hotbar").build(),
                Flag.builder().setRequirement(x -> permissionService.hasPermission(x, ItemPermissions.REPAIR_FLAG_EQUIP)).alias("e").alias("equip").build(),
                Flag.builder().setRequirement(x -> permissionService.hasPermission(x, ItemPermissions.REPAIR_FLAG_OFFHAND)).alias("o").alias("offhand").build()
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final EnumMap<ResultType, Integer> resultCount = new EnumMap<ResultType, Integer>(ResultType.class) {{
            this.put(ResultType.SUCCESS, 0);
            this.put(ResultType.ERROR, 0);
            this.put(ResultType.NEGATIVE_DURABILITY, 0);
            this.put(ResultType.NO_DURABILITY, 0);
            this.put(ResultType.RESTRICTED, 0);
        }};
        final EnumMap<ResultType, ItemStackSnapshot> lastItem = new EnumMap<>(ResultType.class);

        final boolean checkRestrictions = !context.testPermission(ItemPermissions.EXEMPT_REPAIR_RESTRICTION_CHECK);

        final Player pl = context.getIfPlayer();
        String location = "inventory";
        if (context.hasFlag("a")) {
            this.repairInventory(pl.inventory(), checkRestrictions, resultCount, lastItem);
        } else {
            final boolean repairHotbar = context.hasFlag("h");
            final boolean repairEquip = context.hasFlag("e");
            final boolean repairOffhand = context.hasFlag("o");
            final boolean repairMainhand = context.hasFlag("m") || !repairHotbar && !repairEquip && !repairOffhand;

            if (repairHotbar && !repairEquip && !repairOffhand && !repairMainhand) {
                location = "hotbar";
            } else if (repairEquip && !repairHotbar && !repairOffhand && !repairMainhand) {
                location = "equipment";
            } else if (repairOffhand && !repairHotbar && !repairEquip && !repairMainhand) {
                location = "offhand";
            } else if (repairMainhand && !repairHotbar && !repairEquip && !repairOffhand) {
                location = "mainhand";
            }

            // Repair item in main hand
            if (repairMainhand && !pl.itemInHand(HandTypes.MAIN_HAND).isEmpty()) {
                final ItemStack stack = pl.itemInHand(HandTypes.MAIN_HAND);
                final RepairResult result = this.repairStack(stack, checkRestrictions);
                resultCount.compute(result.type, (t, i) -> i += 1);
                lastItem.put(result.type, result.stack.createSnapshot());
                if (result.isSuccessful()) {
                    pl.setItemInHand(HandTypes.MAIN_HAND, result.stack);
                }
            }

            // Repair item in off hand
            if (repairOffhand && !pl.itemInHand(HandTypes.OFF_HAND).isEmpty()) {
                final ItemStack stack = pl.itemInHand(HandTypes.OFF_HAND);
                final RepairResult result = this.repairStack(stack, checkRestrictions);
                resultCount.compute(result.type, (t, i) -> i += 1);
                lastItem.put(result.type, result.stack.createSnapshot());
                if (result.isSuccessful()) {
                    pl.setItemInHand(HandTypes.OFF_HAND, result.stack);
                }
            }

            // Repair worn equipment
            if (repairEquip) {
                this.repairInventory(pl.inventory().equipment(), checkRestrictions, resultCount, lastItem);
            }

            // Repair Hotbar
            if (repairHotbar) {
                this.repairInventory(pl.inventory().hotbar(), checkRestrictions, resultCount, lastItem);
            }
        }

        final String key = "command.repair.location." + location;
        final IMessageProviderService messageProviderService = context.getServiceCollection().messageProvider();
        if (messageProviderService.hasKey(key)) {
            location = messageProviderService.getMessageString(pl, key);
        } else {
            location = "inventory";
        }

        if (resultCount.get(ResultType.SUCCESS) == 0 && resultCount.get(ResultType.ERROR) == 0
                && resultCount.get(ResultType.NO_DURABILITY) == 0 && resultCount.get(ResultType.RESTRICTED) == 0) {
            return context.errorResult("command.repair.empty", pl.name(), location);
        } else {
            // Non-repairable Message - Only used when all items processed had no durability
            final int durabilityCount = resultCount.get(ResultType.NO_DURABILITY) + resultCount.get(ResultType.NEGATIVE_DURABILITY);
            if (durabilityCount > 0 && resultCount.get(ResultType.SUCCESS) == 0
                    && resultCount.get(ResultType.ERROR) == 0 && resultCount.get(ResultType.RESTRICTED) == 0) {
                if (durabilityCount == 1) {
                    ItemStackSnapshot item = lastItem.get(ResultType.NO_DURABILITY);
                    if (item == null) {
                        item = lastItem.get(ResultType.NEGATIVE_DURABILITY);
                    }
                    final Component name = this.getFromItem(item);
                    context.sendMessage(
                            "command.repair.nodurability.single",
                            name.hoverEvent(item),
                            Component.text(pl.name()),
                            Component.text(location)
                    );
                } else {
                    context.sendMessage(
                            "command.repair.nodurability.multiple",
                            resultCount.get(ResultType.NO_DURABILITY).toString(), pl.name(), location
                    );
                }
            }

            // Success Message
            if (resultCount.get(ResultType.SUCCESS) == 1) {
                final ItemStackSnapshot item = lastItem.get(ResultType.SUCCESS);
                final Component name = this.getFromItem(item);
                context.sendMessage(
                        "command.repair.success.single",
                        name,
                        context.getDisplayName(),
                        location
                );
            } else if (resultCount.get(ResultType.SUCCESS) > 1) {
                context.sendMessage(
                        "command.repair.success.multiple",
                        resultCount.get(ResultType.SUCCESS).toString(), pl.name(), location
                );
            }

            // Error Message
            if (resultCount.get(ResultType.ERROR) == 1) {
                final ItemStackSnapshot item = lastItem.get(ResultType.ERROR);
                final Component name = this.getFromItem(item);
                context.sendMessage(
                        "command.repair.error.single",
                        name,
                        context.getDisplayName(),
                        Component.text(location)
                );
            } else if (resultCount.get(ResultType.ERROR) > 1) {
                context.sendMessage(
                        "command.repair.error.multiple",
                        resultCount.get(ResultType.ERROR).toString(), pl.name(), location
                );
            }

            // Restriction Message
            if (resultCount.get(ResultType.RESTRICTED) == 1) {
                final ItemStackSnapshot item = lastItem.get(ResultType.RESTRICTED);
                final Component name = this.getFromItem(item);
                context.sendMessage(
                        "command.repair.restricted.single",
                        name,
                        context.getDisplayName(),
                        Component.text(location)
                );
            } else if (resultCount.get(ResultType.RESTRICTED) > 1) {
                context.sendMessage(
                        "command.repair.restricted.multiple",
                        resultCount.get(ResultType.RESTRICTED).toString(), pl.name(), location
                );
            }

            if (resultCount.get(ResultType.SUCCESS) > 0) {
                return context.successResult();
            } else {
                return context.failResult();
            }
        }
    }

    private void repairInventory(final Inventory inventory, final boolean checkRestrictions,
            final EnumMap<ResultType, Integer> resultCount, final EnumMap<ResultType, ItemStackSnapshot> lastItem) {
        for (final Inventory slot : inventory.slots()) {
            final ItemStack stack = slot.peek();
            if (!stack.isEmpty()) {
                final RepairResult result = this.repairStack(stack, checkRestrictions);
                resultCount.compute(result.type, (t, i) -> i += 1);
                lastItem.put(result.type, result.stack.createSnapshot());
                if (result.isSuccessful()) {
                    slot.offer(result.stack);
                }
            }
        }
    }

    private RepairResult repairStack(final ItemStack stack, final boolean checkRestrictions) {
        if (checkRestrictions && (this.whitelist && !this.restrictions.contains(stack.type()) || this.restrictions.contains(stack.type()))) {
            return new RepairResult(stack, ResultType.RESTRICTED);
        }
        try {
            if (stack.get(Keys.ITEM_DURABILITY).isPresent()) {
                final int maxDurability = stack.get(Keys.MAX_DURABILITY).orElse(Integer.MAX_VALUE);
                final DataTransactionResult transactionResult = stack.offer(Keys.ITEM_DURABILITY, maxDurability);
                if (transactionResult.isSuccessful()) {
                    return new RepairResult(stack, ResultType.SUCCESS);
                } else {
                    return new RepairResult(stack, ResultType.ERROR);
                }
            }
        } catch (final IllegalArgumentException e) {
            return new RepairResult(stack, ResultType.NEGATIVE_DURABILITY);
        }
        return new RepairResult(stack, ResultType.NO_DURABILITY);
    }

    private Component getFromItem(final ItemStackSnapshot stack) {
        return stack.get(Keys.CUSTOM_NAME).orElseGet(() -> stack.type().asComponent()).hoverEvent(stack);
    }

    private enum ResultType {
        SUCCESS, ERROR, RESTRICTED, NEGATIVE_DURABILITY, NO_DURABILITY
    }

    private static class RepairResult {

        private final ItemStack stack;
        private final ResultType type;

        RepairResult(final ItemStack stack, final ResultType type) {
            this.stack = stack;
            this.type = type;
        }

        public boolean isSuccessful() {
            return this.type == ResultType.SUCCESS;
        }
    }

}
