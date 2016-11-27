/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.commands.lore;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.data.manipulator.mutable.item.LoreData;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;

@Permissions(prefix = "lore", mainOverride = "set")
@RegisterCommand(value = "clear", subcommandOf = LoreCommand.class)
public class LoreClearCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<Player> {

    @Inject private MessageProvider provider;

    @Override
    public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        if (!src.getItemInHand(HandTypes.MAIN_HAND).isPresent()) {
            src.sendMessage(provider.getTextMessageWithFormat("command.lore.clear.noitem"));
            return CommandResult.empty();
        }

        ItemStack stack = src.getItemInHand(HandTypes.MAIN_HAND).get();
        LoreData loreData = stack.getOrCreate(LoreData.class).get();
        if (loreData.lore().isEmpty()) {
            src.sendMessage(provider.getTextMessageWithFormat("command.lore.clear.none"));
            return CommandResult.empty();
        }

        if (stack.remove(LoreData.class).isSuccessful()) {
            src.setItemInHand(HandTypes.MAIN_HAND, stack);
            src.sendMessage(provider.getTextMessageWithFormat("command.lore.clear.success"));
            return CommandResult.success();
        }

        src.sendMessage(provider.getTextMessageWithFormat("command.lore.clear.fail"));
        return CommandResult.empty();
    }
}
