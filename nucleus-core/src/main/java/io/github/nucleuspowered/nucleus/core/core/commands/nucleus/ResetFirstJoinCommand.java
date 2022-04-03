/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.core.commands.nucleus;

import io.github.nucleuspowered.nucleus.core.core.CoreKeys;
import io.github.nucleuspowered.nucleus.core.core.CorePermissions;
import io.github.nucleuspowered.nucleus.core.core.commands.NucleusCommand;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.User;

@Command(
        aliases = { "resetfirstjoin" },
        basePermission = CorePermissions.BASE_RESET_FIRST_JOIN,
        commandDescriptionKey = "nucleus.resetfirstjoin",
        parentCommand = NucleusCommand.class
)
public final class ResetFirstJoinCommand implements ICommandExecutor, IReloadableService.Reloadable {

    private boolean useSponge;

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.ONE_USER
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final User targetUser = context.getUserFromArgs(NucleusParameters.ONE_USER);
        context.getServiceCollection().storageManager().getUserService()
                .setAndSave(targetUser.uniqueId(), CoreKeys.FIRST_JOIN_PROCESSED, false)
                .handle((result, exception) -> {
                    if (exception == null) {
                        // all okay
                        context.sendMessage("command.nucleus.firstjoin.success", targetUser.name());
                        if (context.testPermissionFor(targetUser, CorePermissions.EXEMPT_FIRST_JOIN)) {
                            context.sendMessage("command.nucleus.firstjoin.perm", targetUser.name(), CorePermissions.EXEMPT_FIRST_JOIN);
                        }
                        if (this.useSponge && targetUser.get(Keys.FIRST_DATE_JOINED).isPresent()) {
                            context.sendMessage("command.nucleus.firstjoin.date", targetUser.name());
                        }
                    } else {
                        context.sendMessage("command.nucleus.firstjoin.error", targetUser.name(), exception.getMessage());
                        exception.printStackTrace();
                    }
                    return (Void) null;
                });
        return context.successResult();
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        this.useSponge = serviceCollection.configProvider().getCoreConfig().isCheckFirstDatePlayed();
    }

}
