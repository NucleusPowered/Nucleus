/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.commands.lore;

import io.github.nucleuspowered.nucleus.argumentparsers.PositiveIntegerArgument;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.item.LoreData;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.List;

abstract class LoreModifyBaseCommand extends AbstractCommand<Player> {

    final String loreKey = "lore";
    final String loreLine = "line";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                new PositiveIntegerArgument(Text.of(loreLine)),
                GenericArguments.remainingJoinedStrings(Text.of(loreKey))
        };
    }

    /**
     * Like the method above, this method is to update existing lore to the item.
     * When 'editOrInsert' is true, we will edit the lore at the passed line to
     * the passed text. When false, we will rather insert the lore at the specified
     * line.
     *
     * @param src The player attempting to alter an item's lore
     * @param message The text to offer to the item
     * @param line The line of the lore we want to edit
     * @param editOrInsert True to edit, false to insert
     * @return The result of the operation
     */
    CommandResult setLore(Player src, String message, int line, boolean editOrInsert) throws Exception{
        if(line != 0) line--;

        ItemStack stack = src.getItemInHand(HandTypes.MAIN_HAND).orElseThrow(() -> ReturnMessageException.fromKey("command.lore.set.noitem"));
        LoreData loreData = stack.getOrCreate(LoreData.class).get();

        Text getLore = TextSerializers.FORMATTING_CODE.deserialize(message);

        List<Text> loreList = loreData.lore().get();
        if(editOrInsert){
            if(loreList.size() <= line){
                throw ReturnMessageException.fromKey("command.lore.set.invalidEdit");
            }
            loreList.set(line, getLore);
        } else {
            if(loreList.size() <= line){
                loreList.add(getLore);
            } else {
                loreList.add(line, getLore);
            }
        }

        if (stack.offer(Keys.ITEM_LORE, loreList).isSuccessful()) {
            src.setItemInHand(HandTypes.MAIN_HAND, stack);

            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.lore.set.success"));
            return CommandResult.success();
        }

        throw ReturnMessageException.fromKey("command.lore.set.fail");
    }
}
