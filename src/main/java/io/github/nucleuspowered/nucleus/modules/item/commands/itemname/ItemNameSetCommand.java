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
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

@Permissions(prefix = "itemname")
@RegisterCommand(value = "set", subcommandOf = ItemNameCommand.class)
public class ItemNameSetCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<Player> {

    @Inject
    private MessageProvider provider;

    final String nameKey = "name";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.remainingJoinedStrings(Text.of(nameKey))
        };
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        if (!src.getItemInHand(HandTypes.MAIN_HAND).isPresent()) {
            src.sendMessage(provider.getTextMessageWithFormat("command.itemname.set.noitem"));
            return CommandResult.empty();
        }

        ItemStack stack = src.getItemInHand(HandTypes.MAIN_HAND).get();
        Text name = TextSerializers.FORMATTING_CODE.deserialize(args.<String>getOne(nameKey).get());

        if (stack.offer(Keys.DISPLAY_NAME, name).isSuccessful()) {
            src.setItemInHand(HandTypes.MAIN_HAND, stack);

            src.sendMessage(provider.getTextMessageWithFormat("command.itemname.set.success"));
            return CommandResult.success();
        }

        src.sendMessage(provider.getTextMessageWithFormat("command.itemname.set.fail"));
        return CommandResult.empty();
    }
}
