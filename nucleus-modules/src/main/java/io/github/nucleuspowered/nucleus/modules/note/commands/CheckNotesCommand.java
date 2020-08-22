/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.note.NotePermissions;
import io.github.nucleuspowered.nucleus.modules.note.data.NoteData;
import io.github.nucleuspowered.nucleus.modules.note.services.NoteHandler;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@NonnullByDefault
@Command(
        aliases = {"checknotes", "notes"},
        basePermission = NotePermissions.BASE_NOTE,
        commandDescriptionKey = "checknotes",
        async = true
)
public class CheckNotesCommand implements ICommandExecutor<CommandSource> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.ONE_USER.get(serviceCollection)
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        NoteHandler handler = context.getServiceCollection().getServiceUnchecked(NoteHandler.class);
        User user = context.requireOne(NucleusParameters.Keys.USER, User.class);

        List<NoteData> notes = handler.getNotesInternal(user);
        if (notes.isEmpty()) {
            context.sendMessage("command.checknotes.none", user.getName());
            return context.successResult();
        }

        List<Text> messages =
                notes.stream().sorted(Comparator.comparing(NoteData::getDate)).map(x -> createMessage(x, user, context, handler))
                        .collect(Collectors.toList());
        messages.add(0, context.getMessage("command.checknotes.info"));

        PaginationService paginationService = Sponge.getGame().getServiceManager().provideUnchecked(PaginationService.class);
        paginationService.builder()
                .title(
                        Text.builder()
                        .color(TextColors.GOLD)
                        .append(Text.of(context.getMessage("command.checknotes.header", user.getName())))
                        .build())
                .padding(
                        Text.builder()
                        .color(TextColors.YELLOW)
                        .append(Text.of("="))
                        .build())
                .contents(messages)
                .sendTo(context.getCommandSource());

        return context.successResult();
    }

    private Text createMessage(NoteData note,
            User user,
            ICommandContext<? extends CommandSource> context,
            NoteHandler handler) {
        String name;
        if (note.getNoterInternal().equals(Util.CONSOLE_FAKE_UUID)) {
            name = Sponge.getServer().getConsole().getName();
        } else {
            Optional<User> ou = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(note.getNoterInternal());
            name = ou.map(User::getName).orElseGet(() -> context.getMessageString("standard.unknown"));
        }

        //Get the ID of the note, its index in the users List<NoteData>. Add one to start with an ID of 1.
        int id = handler.getNotesInternal(user).indexOf(note) + 1;

        //Action buttons, this should look like 'Action > [Delete] - [Return] <'
        Text.Builder actions = context.getMessage("command.checknotes.action").toBuilder();

        //Add separation between the word 'Action' and action buttons
        actions.append(Text.of(TextColors.GOLD, " > "));

        //Add the delete button [Delete]
        actions.append(Text.builder().append(Text.of(TextColors.RED, context.getMessage("standard.action.delete")))
                .onHover(TextActions.showText(context.getMessage("command.checknotes.hover.delete")))
                .onClick(TextActions.runCommand("/removenote " + user.getName() + " " + id))
                .build());

        //Add a - to separate it from the next action button
        actions.append(Text.of(TextColors.GOLD, " - "));

        //Add the return button [Return]
        actions.append(Text.builder().append(Text.of(TextColors.GREEN, context.getMessage("standard.action.return")))
                .onHover(TextActions.showText(context.getMessage("command.checknotes.hover.return")))
                .onClick(TextActions.runCommand("/checknotes " + user.getName()))
                .build());

        //Add a < to end the actions button list
        actions.append(Text.of(TextColors.GOLD, " < "));

        //Get and format the date of the warning
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.systemDefault());
        String date = dtf.format(note.getDate());

        Text nodeMessage = context.getServiceCollection().textStyleService().addUrls(note.getNote());

        //Create a clickable name providing more information about the warning
        Text.Builder information = Text.builder(name)
                .onHover(TextActions.showText(context.getMessage("command.checknotes.hover.check")))
                .onClick(TextActions.executeCallback(commandSource -> {
                    context.getMessage("command.checknotes.id", String.valueOf(id));
                    context.getMessage( "command.checknotes.date", date);
                    context.getMessage( "command.checknotes.noter", name);
                    context.getMessage( "command.checknotes.note", nodeMessage);
                    commandSource.sendMessage(actions.build());
                }));

        //Create the warning message
        Text.Builder message = Text.builder()
                .append(Text.of(TextColors.GREEN, information.build()))
                .append(Text.of(": "))
                .append(Text.of(TextColors.YELLOW, note.getNote()));


        return message.build();
    }
}
