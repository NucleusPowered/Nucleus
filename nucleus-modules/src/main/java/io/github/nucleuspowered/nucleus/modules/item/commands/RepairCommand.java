/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.commands;

import io.github.nucleuspowered.nucleus.modules.item.ItemPermissions;
import io.github.nucleuspowered.nucleus.modules.item.config.ItemConfig;
import io.github.nucleuspowered.nucleus.modules.item.config.RepairConfig;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.item.DurabilityData;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.equipment.EquipmentInventory;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

@NonnullByDefault
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
public class RepairCommand implements ICommandExecutor<Player>, IReloadableService.Reloadable {

    private boolean whitelist = false;
    private List<ItemType> restrictions = new ArrayList<>();

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        RepairConfig config = serviceCollection.moduleDataProvider().getModuleConfig(ItemConfig.class).getRepairConfig();
        this.whitelist = config.isWhitelist();
        this.restrictions = config.getRestrictions();
    }

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                GenericArguments.flags()
                        .flag("m", "-mainhand")
                        .permissionFlag(ItemPermissions.REPAIR_FLAG_ALL, "a", "-all")
                        .permissionFlag(ItemPermissions.REPAIR_FLAG_HOTBAR, "h", "-hotbar")
                        .permissionFlag(ItemPermissions.REPAIR_FLAG_EQUIP, "e", "-equip")
                        .permissionFlag(ItemPermissions.REPAIR_FLAG_OFFHAND, "o", "-offhand")
                        .buildWith(GenericArguments.none())
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends Player> context) throws CommandException {
        EnumMap<ResultType, Integer> resultCount = new EnumMap<ResultType, Integer>(ResultType.class) {{
            put(ResultType.SUCCESS, 0);
            put(ResultType.ERROR, 0);
            put(ResultType.NEGATIVE_DURABILITY, 0);
            put(ResultType.NO_DURABILITY, 0);
            put(ResultType.RESTRICTED, 0);
        }};
        EnumMap<ResultType, ItemStackSnapshot> lastItem = new EnumMap<>(ResultType.class);

        boolean checkRestrictions = !context.testPermission(ItemPermissions.EXEMPT_REPAIR_RESTRICTION_CHECK);

        Player pl = context.getIfPlayer();
        String location = "inventory";
        if (context.hasAny("a")) {
            repairInventory(pl.getInventory(), checkRestrictions, resultCount, lastItem);
        } else {
            boolean repairHotbar = context.hasAny("h");
            boolean repairEquip = context.hasAny("e");
            boolean repairOffhand = context.hasAny("o");
            boolean repairMainhand = context.hasAny("m") || !repairHotbar && !repairEquip && !repairOffhand;

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
            if (repairMainhand && pl.getItemInHand(HandTypes.MAIN_HAND).isPresent()) {
                ItemStack stack = pl.getItemInHand(HandTypes.MAIN_HAND).get();
                RepairResult result = repairStack(stack, checkRestrictions);
                resultCount.compute(result.type, (t, i) -> i += 1);
                lastItem.put(result.type, result.stack.createSnapshot());
                if (result.isSuccessful()) {
                    pl.setItemInHand(HandTypes.MAIN_HAND, result.stack);
                }
            }

            // Repair item in off hand
            if (repairOffhand && pl.getItemInHand(HandTypes.OFF_HAND).isPresent()) {
                ItemStack stack = pl.getItemInHand(HandTypes.OFF_HAND).get();
                RepairResult result = repairStack(stack, checkRestrictions);
                resultCount.compute(result.type, (t, i) -> i += 1);
                lastItem.put(result.type, result.stack.createSnapshot());
                if (result.isSuccessful()) {
                    pl.setItemInHand(HandTypes.OFF_HAND, result.stack);
                }
            }

            // Repair worn equipment
            if (repairEquip) {
                repairInventory(pl.getInventory()
                        .query(QueryOperationTypes.INVENTORY_TYPE.of(EquipmentInventory.class)), checkRestrictions, resultCount, lastItem);
            }

            // Repair Hotbar
            if (repairHotbar) {
                repairInventory(pl.getInventory()
                        .query(QueryOperationTypes.INVENTORY_TYPE.of(Hotbar.class)), checkRestrictions, resultCount, lastItem);
            }
        }

        String key = "command.repair.location." + location;
        IMessageProviderService messageProviderService = context.getServiceCollection().messageProvider();
        if (messageProviderService.hasKey(key)) {
            location = messageProviderService.getMessageString(pl, key);
        } else {
            location = "inventory";
        }

        if (resultCount.get(ResultType.SUCCESS) == 0 && resultCount.get(ResultType.ERROR) == 0
                && resultCount.get(ResultType.NO_DURABILITY) == 0 && resultCount.get(ResultType.RESTRICTED) == 0) {
            return context.errorResult("command.repair.empty", pl.getName(), location);
        } else {
            // Non-repairable Message - Only used when all items processed had no durability
            int durabilityCount = resultCount.get(ResultType.NO_DURABILITY) + resultCount.get(ResultType.NEGATIVE_DURABILITY);
            if (durabilityCount > 0 && resultCount.get(ResultType.SUCCESS) == 0
                    && resultCount.get(ResultType.ERROR) == 0 && resultCount.get(ResultType.RESTRICTED) == 0) {
                if (durabilityCount == 1) {
                    ItemStackSnapshot item = lastItem.get(ResultType.NO_DURABILITY);
                    if (item == null) {
                        item = lastItem.get(ResultType.NEGATIVE_DURABILITY);
                    }
                    context.sendMessage(
                            "command.repair.nodurability.single",
                            item.get(Keys.DISPLAY_NAME).orElse(Text.of(item.getTranslation().get())).toBuilder()
                                    .onHover(TextActions.showItem(item))
                                    .build(),
                            Text.of(pl.getName()),
                            Text.of(location)
                    );
                } else {
                    context.sendMessage(
                            "command.repair.nodurability.multiple",
                            resultCount.get(ResultType.NO_DURABILITY).toString(), pl.getName(), location
                    );
                }
            }

            // Success Message
            if (resultCount.get(ResultType.SUCCESS) == 1) {
                ItemStackSnapshot item = lastItem.get(ResultType.SUCCESS);
                context.sendMessage(
                        "command.repair.success.single",
                        item.get(Keys.DISPLAY_NAME).orElse(Text.of(item.getTranslation().get())).toBuilder()
                                .onHover(TextActions.showItem(item))
                                .build(),
                        context.getDisplayName(),
                        location
                );
            } else if (resultCount.get(ResultType.SUCCESS) > 1) {
                context.sendMessage(
                        "command.repair.success.multiple",
                        resultCount.get(ResultType.SUCCESS).toString(), pl.getName(), location
                );
            }

            // Error Message
            if (resultCount.get(ResultType.ERROR) == 1) {
                ItemStackSnapshot item = lastItem.get(ResultType.ERROR);
                context.sendMessage(
                        "command.repair.error.single",
                        item.get(Keys.DISPLAY_NAME).orElse(Text.of(item.getTranslation().get())).toBuilder()
                                .onHover(TextActions.showItem(item))
                                .build(),
                        context.getDisplayName(),
                        Text.of(location)
                );
            } else if (resultCount.get(ResultType.ERROR) > 1) {
                context.sendMessage(
                        "command.repair.error.multiple",
                        resultCount.get(ResultType.ERROR).toString(), pl.getName(), location
                );
            }

            // Restriction Message
            if (resultCount.get(ResultType.RESTRICTED) == 1) {
                ItemStackSnapshot item = lastItem.get(ResultType.RESTRICTED);
                context.sendMessage(
                        "command.repair.restricted.single",
                        item.get(Keys.DISPLAY_NAME).orElse(Text.of(item.getTranslation().get())).toBuilder()
                                .onHover(TextActions.showItem(item))
                                .build(),
                        context.getDisplayName(),
                        Text.of(location)
                );
            } else if (resultCount.get(ResultType.RESTRICTED) > 1) {
                context.sendMessage(
                        "command.repair.restricted.multiple",
                        resultCount.get(ResultType.RESTRICTED).toString(), pl.getName(), location
                );
            }

            if (resultCount.get(ResultType.SUCCESS) > 0) {
                return context.successResult();
            } else {
                return context.failResult();
            }
        }
    }

    private void repairInventory(Inventory inventory, boolean checkRestrictions,
            EnumMap<ResultType, Integer> resultCount, EnumMap<ResultType, ItemStackSnapshot> lastItem) {
        for (Inventory slot : inventory.slots()) {
            if (slot.peek().isPresent() && !slot.peek().get().isEmpty()) {
                ItemStack stack = slot.peek().get();
                RepairResult result = repairStack(stack, checkRestrictions);
                resultCount.compute(result.type, (t, i) -> i += 1);
                lastItem.put(result.type, result.stack.createSnapshot());
                if (result.isSuccessful()) {
                    slot.set(result.stack);
                }
            }
        }
    }

    private RepairResult repairStack(ItemStack stack, boolean checkRestrictions) {
        if (checkRestrictions && (this.whitelist && !this.restrictions.contains(stack.getType()) || this.restrictions.contains(stack.getType()))) {
            return new RepairResult(stack, ResultType.RESTRICTED);
        }
        try {
            if (stack.get(DurabilityData.class).isPresent()) {
                DurabilityData durabilityData = stack.get(DurabilityData.class).get();
                DataTransactionResult transactionResult = stack.offer(Keys.ITEM_DURABILITY, durabilityData.durability().getMaxValue());
                if (transactionResult.isSuccessful()) {
                    return new RepairResult(stack, ResultType.SUCCESS);
                } else {
                    return new RepairResult(stack, ResultType.ERROR);
                }
            }
        } catch (IllegalArgumentException e) {
            return new RepairResult(stack, ResultType.NEGATIVE_DURABILITY);
        }
        return new RepairResult(stack, ResultType.NO_DURABILITY);
    }

    private enum ResultType {
        SUCCESS, ERROR, RESTRICTED, NEGATIVE_DURABILITY, NO_DURABILITY;
    }

    private static class RepairResult {

        private final ItemStack stack;
        private final ResultType type;

        RepairResult(ItemStack stack, ResultType type) {
            this.stack = stack;
            this.type = type;
        }

        public boolean isSuccessful() {
            return this.type == ResultType.SUCCESS;
        }
    }
}
