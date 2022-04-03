/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.core.commands.nucleus;

import io.github.nucleuspowered.nucleus.core.core.CorePermissions;
import io.github.nucleuspowered.nucleus.core.core.commands.NucleusCommand;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;

@Command(
        aliases = "clearcache",
        basePermission = CorePermissions.BASE_NUCLEUS_CLEARCACHE,
        commandDescriptionKey = "nucleus.clearcache",
        parentCommand = NucleusCommand.class
)
public class ClearCacheCommand implements ICommandExecutor {

    @Override
    public ICommandResult execute(final ICommandContext context) {
        context.getServiceCollection().storageManager().getUserService().clearCache()
                .whenComplete(
                (complete, exception) -> {
                    if (exception != null) {
                        context.sendMessage("command.nucleus.clearcache.error");
                        context.getServiceCollection().logger().error("Could not clear cache", exception);
                    } else {
                        context.sendMessage("command.nucleus.clearcache.success");
                    }
                }
        );
        return context.successResult();
    }
}