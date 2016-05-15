/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.argumentparsers.KitParser;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCooldown;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCost;
import io.github.nucleuspowered.nucleus.internal.annotations.NoWarmup;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.command.CommandBase;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.kit.config.KitConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.kit.handlers.KitHandler;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;

/**
 * Sets kit items.
 *
 * Command Usage: /kit remove Permission: nucleus.kit.remove.base
 */
@Permissions(root = "kit", suggestedLevel = SuggestedLevel.ADMIN)
@RegisterCommand(value = {"remove", "del", "delete"}, subcommandOf = KitCommand.class)
@RunAsync
@NoWarmup
@NoCooldown
@NoCost
public class KitRemoveCommand extends CommandBase<CommandSource> {

    @Inject private KitHandler kitConfig;
    @Inject private KitConfigAdapter kca;

    private final String kit = "kit";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {GenericArguments.onlyOne(new KitParser(Text.of(kit), kca, kitConfig, false))};
    }

    @Override
    public CommandResult executeCommand(final CommandSource player, CommandContext args) throws Exception {
        KitParser.KitInfo kitName = args.<KitParser.KitInfo>getOne(kit).get();
        kitConfig.removeKit(kitName.name);
        player.sendMessage(Util.getTextMessageWithFormat("command.kit.remove.success", kitName.name));
        return CommandResult.success();
    }
}
