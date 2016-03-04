/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.commands.core;

import io.github.essencepowered.essence.Util;
import io.github.essencepowered.essence.internal.CommandBase;
import io.github.essencepowered.essence.internal.annotations.*;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

@Permissions(root = "essence")
@NoCooldown
@NoCost
@NoWarmup
@RunAsync
@RegisterCommand(value = "reload", subcommandOf = EssenceCommand.class)
public class ReloadCommand extends CommandBase<CommandSource> {
    @Override
    public CommandSpec createSpec() {
        return CommandSpec.builder().executor(this).build();
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        plugin.reload();
        src.sendMessage(Text.of(TextColors.GREEN, Util.getMessageWithFormat("command.reload.one")));
        src.sendMessage(Text.of(TextColors.GREEN, Util.getMessageWithFormat("command.reload.two")));
        return CommandResult.success();
    }
}
