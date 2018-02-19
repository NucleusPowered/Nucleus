/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.item.config.ItemConfigAdapter;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
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
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@NonnullByDefault
@Permissions(supportsOthers = true)
@RegisterCommand({"repair", "mend"})
@EssentialsEquivalent({"repair", "fix"})
public class RepairCommand extends AbstractCommand<Player> implements Reloadable {

    private boolean whitelist = false;
    private List<ItemType> restrictions = new ArrayList<>();

    @Override public void onReload() throws Exception {
        this.whitelist = Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(ItemConfigAdapter.class)
                .getNodeOrDefault().getRepairConfig().isWhitelist();
        this.restrictions = Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(ItemConfigAdapter.class)
                .getNodeOrDefault().getRepairConfig().getRestrictions();
    }

    @Override public CommandElement[] getArguments() {
        return new CommandElement[]{
                GenericArguments.flags()
                        .flag("m", "-mainhand")
                        .permissionFlag(permissions.getPermissionWithSuffix("flag.all"), "a", "-all")
                        .permissionFlag(permissions.getPermissionWithSuffix("flag.hotbar"), "h", "-hotbar")
                        .permissionFlag(permissions.getPermissionWithSuffix("flag.equip"), "e", "-equip")
                        .permissionFlag(permissions.getPermissionWithSuffix("flag.offhand"), "o", "-offhand")
                        .buildWith(GenericArguments.none())
        };
    }

    @Override
    protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> mspi = super.permissionSuffixesToRegister();
        mspi.put("flag.all", PermissionInformation.getWithTranslation("permission.repair.flag.all", SuggestedLevel.ADMIN));
        mspi.put("flag.hotbar", PermissionInformation.getWithTranslation("permission.repair.flag.hotbar", SuggestedLevel.ADMIN));
        mspi.put("flag.equip", PermissionInformation.getWithTranslation("permission.repair.flag.equip", SuggestedLevel.ADMIN));
        mspi.put("flag.offhand", PermissionInformation.getWithTranslation("permission.repair.flag.offhand", SuggestedLevel.ADMIN));
        return mspi;
    }

    @Override protected CommandResult executeCommand(Player pl, CommandContext args) throws Exception {
        EnumMap<ResultType, Integer> resultCount = new EnumMap<ResultType, Integer>(ResultType.class) {{
            put(ResultType.SUCCESS, 0);
            put(ResultType.ERROR, 0);
            put(ResultType.NO_DURABILITY, 0);
            put(ResultType.RESTRICTED, 0);
        }};
        EnumMap<ResultType, ItemStackSnapshot> lastItem = new EnumMap<>(ResultType.class);

        boolean checkRestrictions = !pl.hasPermission(permissions.getPermissionWithSuffix("exempt.restriction"));

        String location = "inventory";
        if (args.hasAny("a")) {
            repairInventory(pl.getInventory(), checkRestrictions, resultCount, lastItem);
        } else {
            boolean repairHotbar = args.hasAny("h");
            boolean repairEquip = args.hasAny("e");
            boolean repairOffhand = args.hasAny("o");
            boolean repairMainhand = args.hasAny("m") || !repairHotbar && !repairEquip && !repairOffhand;

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
                repairInventory(pl.getInventory().query(EquipmentInventory.class), checkRestrictions, resultCount, lastItem);
            }

            // Repair Hotbar
            if (repairHotbar) {
                repairInventory(pl.getInventory().query(Hotbar.class), checkRestrictions, resultCount, lastItem);
            }
        }

        location = plugin.getMessageProvider().getMessageFromKey("command.repair.location." + location).orElse("inventory");

        if (resultCount.get(ResultType.SUCCESS) == 0 && resultCount.get(ResultType.ERROR) == 0
                && resultCount.get(ResultType.NO_DURABILITY) == 0 && resultCount.get(ResultType.RESTRICTED) == 0) {
            throw ReturnMessageException.fromKey("command.repair.empty", pl.getName(), location);
        } else {
            // Non-repairable Message - Only used when all items processed had no durability
            if (resultCount.get(ResultType.NO_DURABILITY) > 0 && resultCount.get(ResultType.SUCCESS) == 0
                    && resultCount.get(ResultType.ERROR) == 0 && resultCount.get(ResultType.RESTRICTED) == 0) {
                if (resultCount.get(ResultType.NO_DURABILITY) == 1) {
                    ItemStackSnapshot item = lastItem.get(ResultType.NO_DURABILITY);
                    pl.sendMessage(plugin.getMessageProvider().getTextMessageWithTextFormat(
                            "command.repair.nodurability.single",
                            item.get(Keys.DISPLAY_NAME).orElse(Text.of(item.getTranslation().get())).toBuilder()
                                    .onHover(TextActions.showItem(item))
                                    .build(),
                            Text.of(pl.getName()),
                            Text.of(location)
                    ));
                } else {
                    pl.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat(
                            "command.repair.nodurability.multiple",
                            resultCount.get(ResultType.NO_DURABILITY).toString(), pl.getName(), location
                    ));
                }
            }

            // Success Message
            if (resultCount.get(ResultType.SUCCESS) == 1) {
                ItemStackSnapshot item = lastItem.get(ResultType.SUCCESS);
                pl.sendMessage(plugin.getMessageProvider().getTextMessageWithTextFormat(
                        "command.repair.success.single",
                        item.get(Keys.DISPLAY_NAME).orElse(Text.of(item.getTranslation().get())).toBuilder()
                                .onHover(TextActions.showItem(item))
                                .build(),
                        Text.of(pl.getName()),
                        Text.of(location)
                ));
            } else if (resultCount.get(ResultType.SUCCESS) > 1) {
                pl.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat(
                        "command.repair.success.multiple",
                        resultCount.get(ResultType.SUCCESS).toString(), pl.getName(), location
                ));
            }

            // Error Message
            if (resultCount.get(ResultType.ERROR) == 1) {
                ItemStackSnapshot item = lastItem.get(ResultType.ERROR);
                pl.sendMessage(plugin.getMessageProvider().getTextMessageWithTextFormat(
                        "command.repair.error.single",
                        item.get(Keys.DISPLAY_NAME).orElse(Text.of(item.getTranslation().get())).toBuilder()
                                .onHover(TextActions.showItem(item))
                                .build(),
                        Text.of(pl.getName()),
                        Text.of(location)
                ));
            } else if (resultCount.get(ResultType.ERROR) > 1) {
                pl.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat(
                        "command.repair.error.multiple",
                        resultCount.get(ResultType.ERROR).toString(), pl.getName(), location
                ));
            }

            // Restriction Message
            if (resultCount.get(ResultType.RESTRICTED) == 1) {
                ItemStackSnapshot item = lastItem.get(ResultType.RESTRICTED);
                pl.sendMessage(plugin.getMessageProvider().getTextMessageWithTextFormat(
                        "command.repair.restricted.single",
                        item.get(Keys.DISPLAY_NAME).orElse(Text.of(item.getTranslation().get())).toBuilder()
                                .onHover(TextActions.showItem(item))
                                .build(),
                        Text.of(pl.getName()),
                        Text.of(location)
                ));
            } else if (resultCount.get(ResultType.RESTRICTED) > 1) {
                pl.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat(
                        "command.repair.restricted.multiple",
                        resultCount.get(ResultType.RESTRICTED).toString(), pl.getName(), location
                ));
            }

            return CommandResult.successCount(resultCount.get(ResultType.SUCCESS));
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
        if (checkRestrictions && (whitelist && !restrictions.contains(stack.getType()) || restrictions.contains(stack.getType()))) {
            return new RepairResult(stack, ResultType.RESTRICTED);
        }
        if (stack.get(DurabilityData.class).isPresent()) {
            DurabilityData durabilityData = stack.get(DurabilityData.class).get();
            DataTransactionResult transactionResult = stack.offer(Keys.ITEM_DURABILITY, durabilityData.durability().getMaxValue());
            if (transactionResult.isSuccessful()) {
                return new RepairResult(stack, ResultType.SUCCESS);
            } else {
                return new RepairResult(stack, ResultType.ERROR);
            }
        } else {
            return new RepairResult(stack, ResultType.NO_DURABILITY);
        }
    }

    private enum ResultType {
        SUCCESS, ERROR, RESTRICTED, NO_DURABILITY
    }

    private class RepairResult {

        private ItemStack stack;
        private ResultType type;

        public RepairResult(ItemStack stack, ResultType type) {
            this.stack = stack;
            this.type = type;
        }

        public boolean isSuccessful() {
            return type == ResultType.SUCCESS;
        }
    }
}
