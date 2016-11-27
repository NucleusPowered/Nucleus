/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.commands.lore;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
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

abstract class LoreSetBaseCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<Player> {

    @Inject private MessageProvider provider;

    final String loreKey = "lore";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.remainingJoinedStrings(Text.of(loreKey))
        };
    }

    CommandResult setLore(Player src, String message, boolean replace) {
        if (!src.getItemInHand(HandTypes.MAIN_HAND).isPresent()) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.lore.set.noitem"));
            return CommandResult.empty();
        }

        ItemStack stack = src.getItemInHand(HandTypes.MAIN_HAND).get();
        LoreData loreData = stack.getOrCreate(LoreData.class).get();

        Text getLore = TextSerializers.FORMATTING_CODE.deserialize(message);

        List<Text> loreList;
        if (replace) {
            loreList = Lists.newArrayList(getLore);
        } else {
            loreList = loreData.lore().get();
            loreList.add(getLore);
        }

        if (stack.offer(Keys.ITEM_LORE, loreList).isSuccessful()) {
            src.setItemInHand(HandTypes.MAIN_HAND, stack);

            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.lore.set.success"));
            return CommandResult.success();
        }

        src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.lore.set.fail"));
        return CommandResult.empty();
    }
}
