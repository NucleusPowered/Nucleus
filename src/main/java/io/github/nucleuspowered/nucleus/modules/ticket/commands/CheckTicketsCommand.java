/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ticket.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.filter.FilterComparator;
import io.github.nucleuspowered.nucleus.api.filter.NucleusTicketFilter;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Ticket;
import io.github.nucleuspowered.nucleus.argumentparsers.TimespanArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.ticket.handlers.TicketHandler;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@NoWarmup
@NoCooldown
@NoCost
@RunAsync
@Permissions(prefix = "ticket", suggestedLevel = SuggestedLevel.MOD)
@RegisterCommand(value = {"check", "c"}, subcommandOf = TicketCommand.class)
public class CheckTicketsCommand extends AbstractCommand<CommandSource> {
    @Inject private TicketHandler handler;

    private final String ownerKey = "owner";
    private final String assigneeKey = "assignee";
    private final String createdSinceKey = "createdsince";
    private final String updatedSinceKey = "updatedsince";
    private final String statusKey = "status";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[]{GenericArguments.flags()
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
        Optional<User> owner = args.getOne(ownerKey);
        Optional<User> assignee = args.getOne(assigneeKey);
        Optional<Long> createdSince = args.getOne(createdSinceKey);
        Optional<Long> updatedSince = args.getOne(updatedSinceKey);
        Optional<Boolean> status = args.getOne(statusKey);

        //Construct the filter.
        NucleusTicketFilter.Builder ticketQueryBuilder = NucleusTicketFilter.builder();
        if (owner.isPresent()) {
            ticketQueryBuilder.filter(NucleusTicketFilter.Property.ID, FilterComparator.EQUALS, owner.get().getUniqueId());
        }
        if (assignee.isPresent()) {
            ticketQueryBuilder.filter(NucleusTicketFilter.Property.ASSIGNEE, FilterComparator.EQUALS, assignee.get().getUniqueId());
        }
        if (createdSince.isPresent()) {
            ticketQueryBuilder.filter(NucleusTicketFilter.Property.CREATION_DATE, FilterComparator.BETWEEN, Instant.now().minusSeconds(createdSince.get()), Instant.now());
        }
        if (updatedSince.isPresent()) {
            ticketQueryBuilder.filter(NucleusTicketFilter.Property.LAST_UPDATE_DATE, FilterComparator.BETWEEN, Instant.now().minusSeconds(updatedSince.get()), Instant.now());
        }
        if (status.isPresent()) {
            ticketQueryBuilder.filter(NucleusTicketFilter.Property.STATUS, FilterComparator.EQUALS, status.get());
        } else { //Get only open tickets by default
            ticketQueryBuilder.filter(NucleusTicketFilter.Property.STATUS, FilterComparator.EQUALS, false);
        }

        src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.checktickets.inprogress"));
        Collection<Ticket> tickets = handler.lookupTicket(ticketQueryBuilder.build()).get();
        if (tickets.isEmpty()) {
            throw ReturnMessageException.fromKey("command.checktickets.none");
        }

        List<Text> messages = tickets.stream().sorted(Comparator.comparing(Ticket::getCreationDate)).map(x -> createMessage(x)).collect(Collectors.toList());
        messages.add(0, plugin.getMessageProvider().getTextMessageWithFormat("command.checktickets.info", String.valueOf(messages.size())));

        PaginationService paginationService = Sponge.getGame().getServiceManager().provideUnchecked(PaginationService.class);
        paginationService.builder()
                .title(
                        Text.builder()
                                .color(TextColors.GOLD)
                                .append(Text.of(plugin.getMessageProvider().getMessageWithFormat("command.checktickets.header")))
                                .build())
                .padding(
                        Text.builder()
                                .color(TextColors.YELLOW)
                                .append(Text.of("="))
                                .build())
                .contents(messages)
                .sendTo(src);

        return CommandResult.success();
    }

    private Text createMessage(Ticket ticket) {
        UUID creator = ticket.getOwner();
        final String creatorName = Util.getUserFromUUID(creator).isPresent() ? Util.getUserFromUUID(creator).get().getName() : StringUtils.capitalize(plugin.getMessageProvider().getMessageWithFormat("standard.unknown"));

        Optional<UUID> assignee = ticket.getAssignee();
        final String assigneeName = assignee.isPresent() ? (Util.getUserFromUUID(assignee.get()).isPresent() ? Util.getUserFromUUID(creator).get().getName() : StringUtils.capitalize(plugin.getMessageProvider().getMessageWithFormat("standard.unassigned"))) : StringUtils.capitalize(plugin.getMessageProvider().getMessageWithFormat("standard.unknown"));

        //Action buttons, for an open ticket this should look like 'Action > [Close/Open] - [Reply] - [Return] <'
        Text.Builder actions = plugin.getMessageProvider().getTextMessageWithFormat("command.checktickets.action").toBuilder();

        //Add separation between the word 'Action' and action buttons
        actions.append(Text.of(TextColors.GOLD, " > "));

        //Add the close or open button depending on the tickets status [Close] or [Open]
        if (!ticket.isClosed()) {
            actions.append(Text.builder().append(Text.of(TextColors.RED, plugin.getMessageProvider().getMessageWithFormat("standard.action.close")))
                    .onHover(TextActions.showText(plugin.getMessageProvider().getTextMessageWithFormat("command.checktickets.hover.close")))
                    .onClick(TextActions.runCommand("/ticket close " + ticket.getId()))
                    .build());
        } else {
            actions.append(Text.builder().append(Text.of(TextColors.GREEN, plugin.getMessageProvider().getMessageWithFormat("standard.action.open")))
                    .onHover(TextActions.showText(plugin.getMessageProvider().getTextMessageWithFormat("command.checktickets.hover.open")))
                    .onClick(TextActions.runCommand("/ticket reopen " + ticket.getId()))
                    .build());
        }

        //Add a - to separate it from the next action button
        actions.append(Text.of(TextColors.GOLD, " - "));

        //Add the reply button [Reply]
        actions.append(Text.builder().append(Text.of(TextColors.YELLOW, plugin.getMessageProvider().getMessageWithFormat("standard.action.reply")))
                .onHover(TextActions.showText(plugin.getMessageProvider().getTextMessageWithFormat("command.checktickets.hover.reply")))
                .onShiftClick(TextActions.insertText("/ticket reply " + ticket.getId() + " <Your Reply>")) //TODO Implement command
                .build());

        //Add a - to separate it from the next action button
        actions.append(Text.of(TextColors.GOLD, " - "));

        //Add the return button [Return]
        actions.append(Text.builder().append(Text.of(TextColors.GREEN, plugin.getMessageProvider().getMessageWithFormat("standard.action.return")))
                .onHover(TextActions.showText(plugin.getMessageProvider().getTextMessageWithFormat("command.checktickets.hover.return")))
                .onClick(TextActions.runCommand("/ticket check")) //TODO Think of a way to do this differently - it will just requery and wont include original params
                .build());

        //Add a < to end the actions button list
        actions.append(Text.of(TextColors.GOLD, " < "));

        //Get and format the creation date of the ticket
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.systemDefault());
        String creationDate = dtf.format(ticket.getCreationDate());

        //Get and format the last updated date of the ticket
        String lastUpdateDate = dtf.format(ticket.getCreationDate());

        //Create a clickable name providing more information about the ticket
        Text.Builder information = Text.builder(String.valueOf(ticket.getId()))
                .onHover(TextActions.showText(plugin.getMessageProvider().getTextMessageWithFormat("command.checktickets.hover.check")))
                .onClick(TextActions.executeCallback(commandSource -> {
                    //Send general ticket related information
                    commandSource.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.checktickets.id", String.valueOf(ticket.getId())));
                    commandSource.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.checktickets.created", creationDate));
                    commandSource.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.checktickets.lastupdated", lastUpdateDate));
                    commandSource.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.checktickets.creator", creatorName));
                    commandSource.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.checktickets.assignee", assigneeName));

                    //Send messages related to ticket
                    commandSource.sendMessage(Text.of(" "));
                    commandSource.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.checktickets.messages", String.valueOf(ticket.getMessages().size())));
                    ticket.getMessages().entrySet().forEach(message -> {
                            Text.Builder ticketMessage = Text.builder().append(Text.of(TextColors.GOLD, TextStyles.UNDERLINE, dtf.format(Instant.ofEpochMilli(message.getKey()))));
                            ticketMessage.append(Text.of(TextColors.GOLD, " - "));
                            ticketMessage.append(Text.of(TextColors.YELLOW, message.getValue()));

                            commandSource.sendMessage(ticketMessage.build());
                    });
                    commandSource.sendMessage(Text.of(" "));

                    //Send action buttons
                    commandSource.sendMessage(actions.build());
                }));

        //Create the ticket message
        Text.Builder message = Text.builder()
                .append(Text.of(TextColors.GREEN, information.build()))
                .append(Text.of(": "))
                .append(Text.of(TextColors.YELLOW, ticket.getMessages().firstEntry().getValue())); //Use first message as title.

        return message.build();
    }
}
