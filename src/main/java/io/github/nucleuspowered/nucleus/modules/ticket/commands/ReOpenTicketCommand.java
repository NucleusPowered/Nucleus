/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ticket.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.exceptions.NoSuchTicketException;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.ticket.handlers.TicketHandler;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;

import java.util.concurrent.CompletableFuture;

@NoWarmup
@NoCooldown
@NoCost
@RunAsync
@Permissions(prefix = "ticket", suggestedLevel = SuggestedLevel.MOD)
@RegisterCommand(value = {"reopen", "open"}, subcommandOf = TicketCommand.class)
public class ReOpenTicketCommand extends AbstractCommand<CommandSource> {
    @Inject
    private TicketHandler ticketHandler;

    private final String ticketIDKey = "id";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[]{
                GenericArguments.onlyOne(GenericArguments.onlyOne(GenericArguments.integer(Text.of(ticketIDKey))))};
    }

    @Override
    protected CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        Integer id = args.<Integer>getOne(ticketIDKey).get();

        CompletableFuture<Boolean> future = ticketHandler.reopenTicket(id);
        future.whenComplete((success, ex) -> {
            if (ex != null) {
                if (ex instanceof NoSuchTicketException) {
                    src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.ticket.noticketid", String.valueOf(id)));
                } else {
                    src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.ticket.lookupexception", String.valueOf(id)));
                }
            } else {
                if (success) {
                    src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.ticket.reopen.success", String.valueOf(id)));
                } else {
                    src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.ticket.reopen.error", String.valueOf(id)));
                }
            }
        });

        return CommandResult.success();
    }
}
