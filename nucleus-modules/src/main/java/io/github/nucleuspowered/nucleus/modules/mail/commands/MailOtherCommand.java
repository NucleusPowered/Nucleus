/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mail.commands;

import io.github.nucleuspowered.nucleus.modules.mail.MailPermissions;
import io.github.nucleuspowered.nucleus.modules.mail.services.MailHandler;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;

@Command(
        aliases = { "other", "o" },
        basePermission = MailPermissions.BASE_MAIL_OTHER,
        commandDescriptionKey = "mail.other",
        parentCommand = MailCommand.class
)
public class MailOtherCommand implements ICommandExecutor {

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.ONE_USER,
                serviceCollection.getServiceUnchecked(MailHandler.class).getMailFilterParameter()
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        return MailReadBase.INSTANCE.executeCommand(
                context,
                context.requireOne(NucleusParameters.ONE_USER),
                context.getAll(context.getServiceCollection().getServiceUnchecked(MailHandler.class).getMailFilterParameter()));
    }
}
