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
import io.github.nucleuspowered.nucleus.modules.item.config.ItemConfigAdapter;
import org.spongepowered.api.Sponge;
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
import org.spongepowered.api.item.inventory.equipment.EquipmentType;
import org.spongepowered.api.item.inventory.equipment.WornEquipmentType;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@NonnullByDefault
@Permissions(supportsOthers = true)
@RegisterCommand({"repair", "mend"})
@EssentialsEquivalent({"repair", "fix"})
public class RepairCommand extends AbstractCommand<Player> implements Reloadable {

    private int successCount = 0;
    private ItemStackSnapshot lastSuccess = ItemStackSnapshot.NONE;
    private int restrictedCount = 0;
    private ItemStackSnapshot lastRestricted = ItemStackSnapshot.NONE;
    private int errorCount = 0;
    private ItemStackSnapshot lastError = ItemStackSnapshot.NONE;
    private int noDurabilityCount = 0;
    private ItemStackSnapshot lastNoDurability = ItemStackSnapshot.NONE;

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
                        .permissionFlag(permissions.getPermissionWithSuffix("all"), "a", "-all")
                        .permissionFlag(permissions.getPermissionWithSuffix("hotbar"), "h", "-hotbar")
                        .permissionFlag(permissions.getPermissionWithSuffix("equip"), "e", "-equip")
                        .permissionFlag(permissions.getPermissionWithSuffix("offhand"), "o", "-offhand")
                        .buildWith(GenericArguments.none())
        };
    }

    @Override protected CommandResult executeCommand(Player pl, CommandContext args) throws Exception {
        String location = "inventory";
        if (args.hasAny("a")) {
            for (Inventory slot : pl.getInventory().slots()) {
                if (slot.peek().isPresent() && !slot.peek().get().isEmpty()) {
                    ItemStack stack = slot.peek().get();
                    repairStack(stack).ifPresent(slot::set);
                }
            }
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
                repairStack(stack).ifPresent(s -> pl.setItemInHand(HandTypes.MAIN_HAND, s));
            }

            // Repair item in off hand
            if (repairOffhand && pl.getItemInHand(HandTypes.OFF_HAND).isPresent()) {
                ItemStack stack = pl.getItemInHand(HandTypes.OFF_HAND).get();
                repairStack(stack).ifPresent(s -> pl.setItemInHand(HandTypes.OFF_HAND, s));
            }

            // Repair worn equipment
            if (repairEquip) {
                for (EquipmentType type : Sponge.getRegistry().getAllOf(WornEquipmentType.class)) {
                    if (pl.getEquipped(type).isPresent()) {
                        ItemStack stack = pl.getEquipped(type).get();
                        repairStack(stack).ifPresent(s -> pl.equip(type, s));
                    }
                }
            }

            // Repair Hotbar
            if (repairHotbar) {
                for (Inventory slot : pl.getInventory().query(Hotbar.class).slots()) {
                    if (slot.peek().isPresent() && !slot.peek().get().isEmpty()) {
                        ItemStack stack = slot.peek().get();
                        repairStack(stack).ifPresent(slot::set);
                    }
                }
            }
        }

        location = plugin.getMessageProvider().getMessageFromKey("repair.command.location." + location).get();
        if (successCount == 0 && errorCount == 0 && noDurabilityCount == 0) {
            throw ReturnMessageException.fromKey("command.repair.empty", pl.getName(), location);
        } else {
            // Non-repairable Message - Only used when all items processed had no durability
            if (noDurabilityCount > 0 && successCount == 0 && errorCount == 0) {
                if (noDurabilityCount == 1) {
                    pl.sendMessage(plugin.getMessageProvider().getTextMessageWithTextFormat(
                            "command.repair.nodurability.single",
                            Text.builder(lastNoDurability.getTranslation().get())
                                    .onHover(TextActions.showItem(lastNoDurability))
                                    .build(),
                            Text.of(pl.getName()),
                            Text.of(location)
                    ));
                } else {
                    pl.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat(
                            "command.repair.nodurability.multiple",
                            Integer.toString(noDurabilityCount), pl.getName(), location
                    ));
                }
            }

            // Success Message
            if (successCount == 1) {
                pl.sendMessage(plugin.getMessageProvider().getTextMessageWithTextFormat(
                        "command.repair.success.single",
                        Text.builder(lastSuccess.getTranslation().get())
                                .onHover(TextActions.showItem(lastSuccess))
                                .build(),
                        Text.of(pl.getName()),
                        Text.of(location)
                ));
            } else if (successCount > 1) {
                pl.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat(
                        "command.repair.success.multiple",
                        Integer.toString(successCount), pl.getName(), location
                ));
            }

            // Error Message
            if (errorCount == 1) {
                pl.sendMessage(plugin.getMessageProvider().getTextMessageWithTextFormat(
                        "command.repair.error.single",
                        Text.builder(lastError.getTranslation().get())
                                .onHover(TextActions.showItem(lastError))
                                .build(),
                        Text.of(pl.getName()),
                        Text.of(location)
                ));
            } else if (errorCount > 1) {
                pl.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat(
                        "command.repair.error.multiple",
                        Integer.toString(errorCount), pl.getName(), location
                ));
            }

            // Restriction Message
            if (restrictedCount == 1) {
                pl.sendMessage(plugin.getMessageProvider().getTextMessageWithTextFormat(
                        "command.repair.restricted.single",
                        Text.builder(lastRestricted.getTranslation().get())
                                .onHover(TextActions.showItem(lastRestricted))
                                .build(),
                        Text.of(pl.getName()),
                        Text.of(location)
                ));
            } else if (restrictedCount > 1) {
                pl.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat(
                        "command.repair.restricted.multiple",
                        Integer.toString(restrictedCount), pl.getName(), location
                ));
            }

            return CommandResult.successCount(successCount);
        }
    }

    private Optional<ItemStack> repairStack(ItemStack stack) {
        if (whitelist && !restrictions.contains(stack.getType()) || restrictions.contains(stack.getType())) {
            restrictedCount += 1;
            lastRestricted = stack.createSnapshot();
            return Optional.empty();
        } else if (stack.get(DurabilityData.class).isPresent()) {
            DurabilityData durabilityData = stack.get(DurabilityData.class).get();
            DataTransactionResult transactionResult = stack.offer(Keys.ITEM_DURABILITY, durabilityData.durability().getMaxValue());
            if (transactionResult.isSuccessful()) {
                successCount += 1;
                lastSuccess = stack.createSnapshot();
                return Optional.of(stack);
            } else {
                errorCount += 1;
                lastError = stack.createSnapshot();
                return Optional.empty();
            }
        } else {
            noDurabilityCount += 1;
            lastNoDurability = stack.createSnapshot();
            return Optional.empty();
        }
    }
}
