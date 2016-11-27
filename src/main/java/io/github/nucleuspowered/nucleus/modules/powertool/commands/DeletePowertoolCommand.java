/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.powertool.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.dataservices.UserService;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Deletes the powertool associated with the item in the hand.
 *
 * Permission: plugin.powertool.base (uses the base permission)
 */
@Permissions(mainOverride = "powertool")
@RunAsync
@NoCooldown
@NoWarmup
@NoCost
@RegisterCommand(value = {"delete", "del", "rm", "remove"}, subcommandOf = PowertoolCommand.class)
public class DeletePowertoolCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<Player> {

    @Inject private UserDataManager loader;

    @Override
    public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        Optional<ItemStack> itemStack = src.getItemInHand(HandTypes.MAIN_HAND);
        if (!itemStack.isPresent()) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.powertool.noitem"));
            return CommandResult.empty();
        }

        UserService user = loader.get(src).get();
        ItemType item = itemStack.get().getItem();

        Optional<List<String>> cmds = user.getPowertoolForItem(item);
        if (cmds.isPresent() && !cmds.get().isEmpty()) {
            user.clearPowertool(item);
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.powertool.removed", item.getId()));
        } else {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.powertool.nocmds", item.getId()));
        }

        return CommandResult.success();
    }
}
