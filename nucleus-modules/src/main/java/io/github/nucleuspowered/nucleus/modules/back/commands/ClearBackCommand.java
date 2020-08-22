/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.back.commands;

import io.github.nucleuspowered.nucleus.modules.back.BackPermissions;
import io.github.nucleuspowered.nucleus.modules.back.services.BackHandler;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.User;

@Command(
        aliases = "clearback",
        basePermission = BackPermissions.BASE_CLEARBACK,
        commandDescriptionKey = "clearback"
)
public class ClearBackCommand implements ICommandExecutor<CommandSource> {

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                GenericArguments.optionalWeak(NucleusParameters.ONE_USER.get(serviceCollection))
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        User target = context.getUserFromArgs();
        boolean isSelf = context.is(target);
        if (!isSelf) {
            if (!context.testPermission(BackPermissions.OTHERS_CLEARBACK)) {
                // no permission
                return context.errorResult("command.clearback.other.noperm");
            }
        }

        context.getServiceCollection().getServiceUnchecked(BackHandler.class).removeLastLocation(target);
        context.sendMessage("command.clearback.success", target.getName());
        return context.successResult();
    }
}
