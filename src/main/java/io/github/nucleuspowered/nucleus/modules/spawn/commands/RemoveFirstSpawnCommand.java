/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.modules.spawn.SpawnKeys;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.util.annotation.NonnullByDefault;

/**
 * plugin.firstspawn.remove.base
 */
@RegisterCommand(value = {"del", "rm"}, subcommandOf = SetFirstSpawnCommand.class)
@Permissions(prefix = "firstspawn", mainOverride = "remove")
@NoModifiers
@RunAsync
@NonnullByDefault
public class RemoveFirstSpawnCommand extends AbstractCommand<CommandSource> {

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) {
        Nucleus.getNucleus().getStorageManager().getGeneralService()
                .getOrNewOnThread()
                .remove(SpawnKeys.FIRST_SPAWN_LOCATION);
        src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.setfirstspawn.remove"));
        return CommandResult.success();
    }
}
