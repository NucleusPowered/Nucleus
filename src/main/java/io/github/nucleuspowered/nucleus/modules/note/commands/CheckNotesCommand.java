/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.data.NoteData;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import io.github.nucleuspowered.nucleus.internal.command.CommandBase;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.note.handlers.NoteHandler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Checks the notes of a player.
 *
 * Command Usage: /checknotes user Permission: quickstart.checknotes.base
 */
@Permissions(suggestedLevel = SuggestedLevel.MOD)
@RunAsync
@NoWarmup
@NoCooldown
@NoCost
@RegisterCommand({"checknotes", "notes"})
public class CheckNotesCommand extends CommandBase<CommandSource> {

    @Inject private NoteHandler handler;
    private final String playerKey = "player";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {GenericArguments.onlyOne(GenericArguments.user(Text.of(playerKey)))};
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        User user = args.<User>getOne(playerKey).get();

        List<NoteData> notes = handler.getNotes(user);
        if (notes.isEmpty()) {
            src.sendMessage(Util.getTextMessageWithFormat("command.checknotes.none", user.getName()));
            return CommandResult.success();
        }

        List<Text> messages = notes.stream().sorted((a, b) -> a.getDate().compareTo(b.getDate())).map(x -> createMessage(x, user)).collect(Collectors.toList());
        messages.add(0, Util.getTextMessageWithFormat("command.checknotes.info"));

        PaginationService paginationService = Sponge.getGame().getServiceManager().provideUnchecked(PaginationService.class);
        paginationService.builder()
                .title(
                        Text.builder()
                        .color(TextColors.GOLD)
                        .append(Text.of(Util.getMessageWithFormat("command.checknotes.header", user.getName())))
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

    private Text createMessage(NoteData note, User user) {
        String name;
        if (note.getNoter().equals(Util.consoleFakeUUID)) {
            name = Sponge.getServer().getConsole().getName();
        } else {
            Optional<User> ou = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(note.getNoter());
            name = ou.isPresent() ? ou.get().getName() : Util.getMessageWithFormat("standard.unknown");
        }

        //Get the ID of the note, its index in the users List<NoteData>
        int id = handler.getNotes(user).indexOf(note);

        //Action buttons, this should look like 'Action > [Delete] - [Return] <'
        Text.Builder actions = Util.getTextMessageWithFormat("command.checknotes.action").toBuilder();

        //Add separation between the word 'Action' and action buttons
        actions.append(Text.of(TextColors.GOLD, " > "));

        //Add the delete button [Delete]
        actions.append(Text.builder().append(Text.of(TextColors.RED, Util.getMessageWithFormat("standard.action.delete")))
                .onHover(TextActions.showText(Util.getTextMessageWithFormat("command.checknotes.hover.delete")))
                .onClick(TextActions.runCommand("/removenote " + user.getName() + " " + id))
                .build());

        //Add a - to separate it from the next action button
        actions.append(Text.of(TextColors.GOLD, " - "));

        //Add the return button [Return]
        actions.append(Text.builder().append(Text.of(TextColors.GREEN, Util.getMessageWithFormat("standard.action.return")))
                .onHover(TextActions.showText(Util.getTextMessageWithFormat("command.checknotes.hover.return")))
                .onClick(TextActions.runCommand("/checknotes " + user.getName()))
                .build());

        //Add a < to end the actions button list
        actions.append(Text.of(TextColors.GOLD, " < "));

        //Get and format the date of the warning
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.systemDefault());
        String date = dtf.format(note.getDate());

        //Create a clickable name providing more information about the warning
        Text.Builder information = Text.builder(name)
                .onHover(TextActions.showText(Util.getTextMessageWithFormat("command.checknotes.hover.check")))
                .onClick(TextActions.executeCallback(commandSource -> {
                    commandSource.sendMessage(Util.getTextMessageWithFormat("command.checknotes.id", String.valueOf(id)));
                    commandSource.sendMessage(Util.getTextMessageWithFormat("command.checknotes.date", date));
                    commandSource.sendMessage(Util.getTextMessageWithFormat("command.checknotes.noter", name));
                    commandSource.sendMessage(Util.getTextMessageWithFormat("command.checknotes.note", note.getNote()));
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
