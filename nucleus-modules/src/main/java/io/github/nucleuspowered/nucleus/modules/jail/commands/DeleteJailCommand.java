/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.commands;

import io.github.nucleuspowered.nucleus.datatypes.LocationData;
import io.github.nucleuspowered.nucleus.modules.jail.JailParameters;
import io.github.nucleuspowered.nucleus.modules.jail.JailPermissions;
import io.github.nucleuspowered.nucleus.modules.jail.services.JailHandler;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import javax.inject.Inject;

@EssentialsEquivalent({"deljail", "remjail", "rmjail"})
@NonnullByDefault
@Command(
        aliases = {"delete", "del", "remove", "#deljail", "#rmjail", "#deletejail" },
        basePermission = JailPermissions.BASE_JAILS_DELETE,
        async = true,
        commandDescriptionKey = "jails.delete",
        parentCommand = JailsCommand.class
)
public class DeleteJailCommand implements ICommandExecutor<CommandSource> {

    private final JailHandler handler;

    @Inject
    public DeleteJailCommand(INucleusServiceCollection serviceCollection) {
        this.handler = serviceCollection.getServiceUnchecked(JailHandler.class);
    }

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                JailParameters.JAIL.get(serviceCollection)
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        LocationData wl = context.requireOne(JailParameters.JAIL_KEY, LocationData.class);
        if (this.handler.removeJail(wl.getName())) {
            context.sendMessage("command.jails.del.success", wl.getName());
            return context.successResult();
        }

        return context.errorResult("command.jails.del.error", wl.getName());
    }
}
