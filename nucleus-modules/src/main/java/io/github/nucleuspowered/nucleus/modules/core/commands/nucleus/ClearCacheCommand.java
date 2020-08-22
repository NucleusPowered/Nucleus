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
        aliases = "nucleus",
        basePermission = CorePermissions.BASE_NUCLEUS_CLEARCACHE,
        commandDescriptionKey = "nucleus.clearcache",
        parentCommand = NucleusCommand.class,
        async = true
)
public class ClearCacheCommand implements ICommandExecutor<CommandSource> {

    @Override
    public ICommandResult execute(final ICommandContext<? extends CommandSource> context) {
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