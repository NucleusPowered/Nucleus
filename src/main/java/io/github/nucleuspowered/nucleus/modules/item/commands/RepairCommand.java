/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.commands;

import io.github.nucleuspowered.nucleus.argumentparsers.SelectorWrapperArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.item.DurabilityData;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

import java.util.HashMap;
import java.util.Map;

@Permissions(supportsSelectors = true)
@RegisterCommand({"repair", "mend"})
public class RepairCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<CommandSource> {

    private final String player = "player";

    @Override
    public Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("others", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.others", this.getAliases()[0]), SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.optional(
                GenericArguments.requiringPermission(GenericArguments.onlyOne(
                    new SelectorWrapperArgument(GenericArguments.player(Text.of(player)), permissions, SelectorWrapperArgument.SINGLE_PLAYER_SELECTORS)),
                permissions.getPermissionWithSuffix("others")))
        };
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        Player pl = this.getUserFromArgs(Player.class, src, player, args);
        if (pl.getItemInHand(HandTypes.MAIN_HAND).isPresent()) {
            ItemStack stack = pl.getItemInHand(HandTypes.MAIN_HAND).get();
            if (stack.get(DurabilityData.class).isPresent()) {
                DurabilityData durabilityData = stack.get(DurabilityData.class).get();
                DataTransactionResult transactionResult = stack.offer(Keys.ITEM_DURABILITY, durabilityData.durability().getMaxValue());
                if (transactionResult.isSuccessful()) {
                    pl.setItemInHand(HandTypes.MAIN_HAND, stack);
                    src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.repair.success", pl.getName()));
                    return CommandResult.success();
                } else {
                    src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.repair.error"));
                    return CommandResult.empty();
                }
            } else {
                src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.repair.error.notreparable"));
                return CommandResult.empty();
            }
        } else {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.repair.error.handempty"));
            return CommandResult.empty();
        }
    }
}
