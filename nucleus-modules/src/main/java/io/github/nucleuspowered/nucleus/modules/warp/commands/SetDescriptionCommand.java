/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.commands;

import io.github.nucleuspowered.nucleus.modules.warp.WarpPermissions;
import io.github.nucleuspowered.nucleus.modules.warp.services.WarpService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.Flag;

@Command(
        aliases = {"setdescription"},
        basePermission = WarpPermissions.BASE_WARP_SETDESCRIPTION,
        commandDescriptionKey = "warp.setdescription",
        parentCommand = WarpCommand.class
)
public class SetDescriptionCommand implements ICommandExecutor {

    @Override public Flag[] flags(final INucleusServiceCollection serviceCollection) {
        return new Flag[] {
                Flag.of("r", "remove", "delete")
        };
    }

    @Override public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                serviceCollection.getServiceUnchecked(WarpService.class).warpElement(false),
                NucleusParameters.OPTIONAL_DESCRIPTION_COMPONENT
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final WarpService handler = context.getServiceCollection().getServiceUnchecked(WarpService.class);
        final String warpName = context.requireOne(handler.warpElement(false)).getNamedLocation().getName();
        if (context.hasFlag("r")) {
            // Remove the desc.
            if (handler.setWarpDescription(warpName, null)) {
                context.sendMessage("command.warp.description.removed", warpName);
                return context.successResult();
            }

            return context.errorResult("command.warp.description.noremove", warpName);
        }

        // Add the category.
        final Component message = context.requireOne(NucleusParameters.OPTIONAL_DESCRIPTION_COMPONENT);
        if (handler.setWarpDescription(warpName, message)) {
            context.sendMessage("command.warp.description.added", message, Component.text(warpName));
            return context.successResult();
        }

        return context.errorResult("command.warp.description.couldnotadd", Component.text(warpName));
    }
}
