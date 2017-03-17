/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.commands.lore;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.argumentparsers.MessageTargetArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.NicknameArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.SelectorWrapperArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.item.LoreData;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

import java.util.List;

@Permissions(prefix = "lore", mainOverride = "set")
@RegisterCommand(value = "delete", subcommandOf = LoreCommand.class)
public class LoreDeleteCommand extends AbstractCommand<Player> {

    @Inject private MessageProvider provider;

    private final String loreLine = "line";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.integer(Text.of(loreLine))
        };
    }

    @Override
    protected CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        int line = args.<Integer>getOne(loreLine).get();
        if(line < 0){
            src.sendMessage(provider.getTextMessageWithFormat("command.lore.set.invalidLine"));
            return CommandResult.empty();
        }

        if (!src.getItemInHand(HandTypes.MAIN_HAND).isPresent()) {
            src.sendMessage(provider.getTextMessageWithFormat("command.lore.clear.noitem"));
            return CommandResult.empty();
        }

        ItemStack stack = src.getItemInHand(HandTypes.MAIN_HAND).get();
        LoreData loreData = stack.getOrCreate(LoreData.class).get();

        List<Text> loreList = loreData.lore().get();
        if(loreList.size() <= line){
            src.sendMessage(provider.getTextMessageWithFormat("command.lore.set.invalidLine"));
            return CommandResult.empty();
        }

        loreList.remove(line);

        if (stack.offer(Keys.ITEM_LORE, loreList).isSuccessful()) {
            src.setItemInHand(HandTypes.MAIN_HAND, stack);

            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.lore.set.success"));
            return CommandResult.success();
        }

        src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.lore.set.fail"));
        return CommandResult.empty();
    }
}
