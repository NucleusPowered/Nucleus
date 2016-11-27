/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.powertool.commands;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.dataservices.UserService;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCooldown;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCost;
import io.github.nucleuspowered.nucleus.internal.annotations.NoWarmup;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.List;
import java.util.Optional;

/**
 * Creates or destroys a powertool based on the item in the player's hand.
 *
 * Command - /powertool del|[command].
 */
@Permissions
@RunAsync
@NoCooldown
@NoWarmup
@NoCost
@RegisterCommand({"powertool", "pt"})
public class PowertoolCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<Player> {

    @Inject private UserDataManager loader;
    private final String commandKey = "command";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of(commandKey)))};
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        Optional<ItemStack> itemStack = src.getItemInHand(HandTypes.MAIN_HAND);
        if (!itemStack.isPresent()) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.powertool.noitem"));
            return CommandResult.empty();
        }

        Optional<String> command = args.getOne(commandKey);
        UserService inu = loader.get(src).get();
        return command.isPresent() ? setPowertool(src, inu, itemStack.get().getItem(), command.get())
                : viewPowertool(src, inu, itemStack.get().getItem());
    }

    private CommandResult viewPowertool(Player src, UserService user, ItemType item) {
        Optional<List<String>> cmds = user.getPowertoolForItem(item);
        if (cmds.isPresent() && !cmds.get().isEmpty()) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.powertool.viewcmds", item.getId()));
            cmds.get().forEach(f -> src.sendMessage(Text.of(TextColors.YELLOW, f)));
        } else {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.powertool.nocmds", item.getId()));
        }

        return CommandResult.success();
    }

    private CommandResult setPowertool(Player src, UserService user, ItemType item, String command) {
        // For consistency, if a command starts with "/", remove it, but just
        // once. WorldEdit commands can be input using "//"
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        user.setPowertool(item, Lists.newArrayList(command));
        src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.powertool.set", item.getId(), command));
        return CommandResult.success();
    }
}
