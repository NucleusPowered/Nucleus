/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mail.commands;

import io.github.nucleuspowered.nucleus.modules.mail.MailPermissions;
import io.github.nucleuspowered.nucleus.modules.mail.services.MailHandler;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
@Command(
        aliases = { "send", "s", "#sendmail" },
        basePermission = MailPermissions.BASE_MAIL_SEND,
        commandDescriptionKey = "mail.send",
        async = true,
        parentCommand = MailCommand.class
)
public class SendMailCommand implements ICommandExecutor<CommandSource> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.ONE_USER.get(serviceCollection),
                NucleusParameters.MESSAGE
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        User pl = context.getOne(NucleusParameters.Keys.USER, User.class)
                .orElseThrow(() -> context.createException("args.user.none"));

        // Only send mails to players that can read them.
        if (!context.testPermissionFor(pl, MailPermissions.BASE_MAIL)) {
            return context.errorResult("command.mail.send.error", pl.getName());
        }

        // Send the message.
        String m = context.getOne(NucleusParameters.Keys.MESSAGE, String.class)
                .orElseThrow(() -> context.createException("args.message.none"));
        MailHandler handler = context.getServiceCollection().getServiceUnchecked(MailHandler.class);
        if (context.is(Player.class)) {
            handler.sendMail(context.getIfPlayer(), pl, m);
        } else {
            handler.sendMailFromConsole(pl, m);
        }

        return context.errorResult("command.mail.send.successful", pl.getName());
    }
}
