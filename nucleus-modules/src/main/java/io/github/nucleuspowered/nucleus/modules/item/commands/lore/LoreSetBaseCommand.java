/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.commands.lore;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.item.LoreData;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.List;

@NonnullByDefault
abstract class LoreSetBaseCommand implements ICommandExecutor<Player> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.LORE
        };
    }

    ICommandResult setLore(ICommandContext<? extends Player> context, String message, boolean replace) throws CommandException {
        Player src = context.getIfPlayer();
        ItemStack stack = src.getItemInHand(HandTypes.MAIN_HAND).orElseThrow(() -> context.createException("command.lore.set.noitem"));
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

            context.sendMessage("command.lore.set.success");
            return context.successResult();
        }

        return context.errorResult("command.lore.set.fail");
    }
}
