/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.commands;

import io.github.nucleuspowered.nucleus.api.module.warp.data.Warp;
import io.github.nucleuspowered.nucleus.modules.warp.WarpPermissions;
import io.github.nucleuspowered.nucleus.modules.warp.services.WarpService;
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
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
@Command(
        aliases = {"setdescription"},
        basePermission = WarpPermissions.BASE_WARP_SETDESCRIPTION,
        commandDescriptionKey = "warp.setdescription",
        async = true,
        parentCommand = WarpCommand.class
)
public class SetDescriptionCommand implements ICommandExecutor<CommandSource> {

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
            GenericArguments.flags().flag("r", "-remove", "-delete").buildWith(
                GenericArguments.seq(
                        serviceCollection.getServiceUnchecked(WarpService.class).warpElement(false),
                        NucleusParameters.OPTIONAL_DESCRIPTION
                )
            )
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        WarpService handler = context.getServiceCollection().getServiceUnchecked(WarpService.class);
        String warpName = context.requireOne(WarpService.WARP_KEY, Warp.class).getName();
        if (context.hasAny("r")) {
            // Remove the desc.
            if (handler.setWarpDescription(warpName, null)) {
                context.sendMessage("command.warp.description.removed", warpName);
                return context.successResult();
            }

            return context.errorResult("command.warp.description.noremove", warpName);
        }

        // Add the category.
        Text message = TextSerializers.FORMATTING_CODE.deserialize(context.requireOne(NucleusParameters.Keys.DESCRIPTION, String.class));
        if (handler.setWarpDescription(warpName, message)) {
            context.sendMessage("command.warp.description.added", message, Text.of(warpName));
            return context.successResult();
        }

        return context.errorResult("command.warp.description.couldnotadd", Text.of(warpName));
    }
}
