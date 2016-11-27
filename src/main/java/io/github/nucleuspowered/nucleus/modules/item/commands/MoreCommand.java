/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.commands;

import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;

@Permissions
@RegisterCommand({"more", "stack"})
public class MoreCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<Player> {

    @Override
    public CommandResult executeCommand(Player player, CommandContext args) throws Exception {

        if (player.getItemInHand(HandTypes.MAIN_HAND).isPresent()) {
            ItemStack stack = player.getItemInHand(HandTypes.MAIN_HAND).get();
            stack.setQuantity(stack.getMaxStackQuantity());
            player.setItemInHand(HandTypes.MAIN_HAND, stack);
            player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.more.success", stack.getItem().getName(),
                    String.valueOf(stack.getItem().getMaxStackQuantity())));
            return CommandResult.success();
        }

        player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.more.none"));
        return CommandResult.empty();
    }
}
