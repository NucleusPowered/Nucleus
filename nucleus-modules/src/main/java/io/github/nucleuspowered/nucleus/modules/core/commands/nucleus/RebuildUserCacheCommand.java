/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.commands.nucleus;

import io.github.nucleuspowered.nucleus.modules.core.CorePermissions;
import io.github.nucleuspowered.nucleus.modules.core.commands.NucleusCommand;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import org.spongepowered.api.command.CommandSource;

@Command(
        aliases = "rebuildusercache",
        basePermission = CorePermissions.BASE_NUCLEUS_REBUILDUSERCACHE,
        commandDescriptionKey = "nucleus.rebuildusercache",
        parentCommand = NucleusCommand.class,
        async = true
)
public class RebuildUserCacheCommand implements ICommandExecutor<CommandSource> {

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) {
        context.sendMessage("command.nucleus.rebuild.start");
        if (context.getServiceCollection().userCacheService().fileWalk()) {
            context.sendMessage("command.nucleus.rebuild.end");
            return context.successResult();
        } else {
            return context.errorResult("command.nucleus.rebuild.fail");
        }
    }
}
