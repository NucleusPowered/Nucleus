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
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;
import org.spongepowered.api.item.inventory.equipment.WornEquipmentType;
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
    private int errorCount = 0;
    private int noRepairCount = 0;

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

        if (successCount == 0 && errorCount == 0 && noRepairCount == 0) {
            throw ReturnMessageException.fromKey("command.repair.error.handempty");
        } else if (successCount > 0) {
            pl.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.repair.success", pl.getName()));
            return CommandResult.successCount(successCount);
        } else if (errorCount > 0) {
            throw ReturnMessageException.fromKey("command.repair.error");
        } else {
            throw ReturnMessageException.fromKey("command.repair.error.notreparable");
        }
    }

    private Optional<ItemStack> repairStack(ItemStack stack) {
        if (whitelist && !restrictions.contains(stack.getType()) || restrictions.contains(stack.getType())) {
            return Optional.empty();
        }
        if (stack.get(DurabilityData.class).isPresent()) {
            DurabilityData durabilityData = stack.get(DurabilityData.class).get();
            DataTransactionResult transactionResult = stack.offer(Keys.ITEM_DURABILITY, durabilityData.durability().getMaxValue());
            if (transactionResult.isSuccessful()) {
                successCount += 1;
                return Optional.of(stack);
            } else {
                errorCount += 1;
                return Optional.empty();
            }
        } else {
            noRepairCount += 1;
            return Optional.empty();
        }
    }
}
