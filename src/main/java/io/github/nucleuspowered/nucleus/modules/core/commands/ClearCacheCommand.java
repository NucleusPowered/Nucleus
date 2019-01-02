/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.event.cause.Cause;

/**
 * Clears the {@link UserDataManager} cache, so any offline user's files wll be read on next login.
 */
@SuppressWarnings("ALL")
@Permissions(prefix = "nucleus")
@RunAsync
@NoModifiers
@RegisterCommand(value = "clearcache", subcommandOf = NucleusCommand.class)
public class ClearCacheCommand extends AbstractCommand<CommandSource> {

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) throws Exception {
        Nucleus.getNucleus().getStorageManager().getUserService().clearCache().whenComplete(
                (complete, exception) -> {
                    if (exception != null) {
                        sendMessageTo(src, "command.nucleus.clearcache.error");
                        Nucleus.getNucleus().getLogger().error("Could not clear cache", exception);
                    } else {
                        sendMessageTo(src, "command.nucleus.clearcache.success");
                    }
                }
        );
        return CommandResult.success();
    }
}