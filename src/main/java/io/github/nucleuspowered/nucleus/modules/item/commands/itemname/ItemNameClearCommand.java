/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.commands.itemname;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

import java.util.Optional;

@Permissions(prefix = "itemname")
@RegisterCommand(value = "clear", subcommandOf = ItemNameCommand.class)
public class ItemNameClearCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<Player> {

    @Inject private MessageProvider provider;

    @Override
    public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        if (!src.getItemInHand(HandTypes.MAIN_HAND).isPresent()) {
            src.sendMessage(provider.getTextMessageWithFormat("command.itemname.clear.noitem"));
            return CommandResult.empty();
        }

        ItemStack stack = src.getItemInHand(HandTypes.MAIN_HAND).get();
        Optional<Text> data = stack.get(Keys.DISPLAY_NAME);

        if (!data.isPresent()) {
            // No display name.
            src.sendMessage(provider.getTextMessageWithFormat("command.lore.clear.none"));
            return CommandResult.empty();
        }

        if (stack.remove(Keys.DISPLAY_NAME).isSuccessful()) {
            src.setItemInHand(HandTypes.MAIN_HAND, stack);
            src.sendMessage(provider.getTextMessageWithFormat("command.itemname.clear.success"));
            return CommandResult.success();
        }

        src.sendMessage(provider.getTextMessageWithFormat("command.itemname.clear.fail"));
        return CommandResult.empty();
    }
}
