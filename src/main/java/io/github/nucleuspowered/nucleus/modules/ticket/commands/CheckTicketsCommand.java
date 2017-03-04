/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ticket.commands;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Ticket;
import io.github.nucleuspowered.nucleus.argumentparsers.TimespanArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.ticket.handlers.TicketHandler;
import org.apache.commons.lang3.tuple.Triple;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Checks the tickets database.
 *
 * Command Usage: /tickets filters Permission: plugin.tickets.base
 */
@Permissions(suggestedLevel = SuggestedLevel.MOD)
@RunAsync
@NoWarmup
@NoCooldown
@NoCost
@RegisterCommand({"tickets", "checktickets"})
public class CheckTicketsCommand extends AbstractCommand<CommandSource> {
    @Inject private TicketHandler handler;
    private final String ownerKey = "owner";
    private final String assigneeKey = "assignee";
    private final String createdSinceKey = "createdsince";
    private final String updatedSinceKey = "updatedsince";
    private final String statusKey = "status";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {GenericArguments.flags()
                .valueFlag(GenericArguments.user(Text.of(ownerKey)), "-owner", "o")
                .valueFlag(GenericArguments.user(Text.of(assigneeKey)), "-assignee", "a")
                .valueFlag(GenericArguments.onlyOne(new TimespanArgument(Text.of(createdSinceKey))), "-created", "c")
                .valueFlag(GenericArguments.onlyOne(new TimespanArgument(Text.of(updatedSinceKey))), "-updated", "u")
                .valueFlag(GenericArguments.bool(Text.of(statusKey)), "-status", "s")
                .buildWith(GenericArguments.none())
        };
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        List<Triple<String, String, String>> arguments = Lists.newArrayList();
        Optional<User> owner = args.getOne(ownerKey);
        Optional<User> assignee = args.getOne(assigneeKey);
        Optional<Integer> createdSince = args.getOne(createdSinceKey);
        Optional<Integer> updatedSince = args.getOne(updatedSinceKey);
        Optional<Boolean> status = args.getOne(statusKey);

        if (owner.isPresent()) arguments.add(Triple.of("Owner", "=", owner.get().getUniqueId().toString()));
        if (assignee.isPresent()) arguments.add(Triple.of("Assignee", "=", assignee.get().getUniqueId().toString()));
        if (createdSince.isPresent()) arguments.add(Triple.of("CreationDate", "BETWEEN", "(" + Timestamp.from(Instant.now().minusSeconds(createdSince.get())) + " AND " + Timestamp.from(Instant.now()) + ")"));
        if (updatedSince.isPresent()) arguments.add(Triple.of("LastUpdateDate", "BETWEEN", "(" + Timestamp.from(Instant.now().minusSeconds(updatedSince.get())) + " AND " + Timestamp.from(Instant.now()) + ")"));
        if (status.isPresent()) arguments.add(Triple.of("Closed", "=", String.valueOf(status.get())));

        CompletableFuture<List<Ticket>> tickets = handler.getTicketsByArguments(arguments);
        //TODO Check, format, send.

        return CommandResult.empty();
    }
}
