/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note.commands;

import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.api.module.note.data.Note;
import io.github.nucleuspowered.nucleus.modules.note.NotePermissions;
import io.github.nucleuspowered.nucleus.modules.note.services.NoteService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.util.Identifiable;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Command(
        aliases = {"checknotes", "notes"},
        basePermission = NotePermissions.BASE_NOTE,
        commandDescriptionKey = "checknotes")
public class CheckNotesCommand implements ICommandExecutor {

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.Composite.USER_OR_GAME_PROFILE
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final NoteService handler = context.getServiceCollection().getServiceUnchecked(NoteService.class);
        final UUID uuid = NucleusParameters.Composite.parseUserOrGameProfile(context).fold(Function.identity(), Identifiable::uniqueId);

        handler.getNotes(uuid).thenAccept(notes -> {
            final Component name = context.getServiceCollection().playerDisplayNameService().getName(uuid);
            if (notes.isEmpty()) {
                context.sendMessage("command.checknotes.none", name);
                return;
            }

            final List<Component> messages =
                    notes.stream().sorted(Comparator.comparing(Note::getDate)).map(x ->
                            this.createMessage(new ArrayList<>(notes), x, uuid, context))
                            .collect(Collectors.toList());
            messages.add(0, context.getMessage("command.checknotes.info"));

            context.getServiceCollection().schedulerService().runOnMainThread(() ->
                    Util.getPaginationBuilder(context.audience())
                        .title(
                                context.getMessage("command.checknotes.header", name))
                        .padding(Component.text("=", NamedTextColor.YELLOW))
                        .contents(messages)
                        .sendTo(context.audience()));
        });
        return context.successResult();
    }

    private Component createMessage(
            final List<Note> notes,
            final Note note,
            final UUID uuid,
            final ICommandContext context) {
        final Component noter = context.getDisplayName(note.getNoter().orElse(Util.CONSOLE_FAKE_UUID));

        //Get the ID of the note, its index in the users List<NoteData>. Add one to start with an ID of 1.
        final int id = notes.indexOf(note) + 1;

        //Add the delete button [Delete]
        final Component actions = LinearComponents.linear(
                    context.getMessage("standard.action.delete").color(NamedTextColor.RED)
                            .hoverEvent(HoverEvent.showText(context.getMessage("command.checknotes.hover.delete")))
                            .clickEvent(ClickEvent.runCommand("/removenote " + uuid.toString() + " " + id)),
                    Component.text(" - ", NamedTextColor.GOLD),
                    context.getMessage("standard.action.return").color(NamedTextColor.GREEN)
                        .hoverEvent(HoverEvent.showText(context.getMessage("command.checknotes.hover.return")))
                        .clickEvent(ClickEvent.runCommand("/nucleus:checknotes " + uuid.toString()))
                );

        //Get and format the date of the warning
        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.systemDefault());
        final String date = dtf.format(note.getDate());

        final Component nodeMessage = context.getServiceCollection().textStyleService().addUrls(note.getNote());

        //Create a clickable name providing more information about the note
        final Component information = noter
                .hoverEvent(HoverEvent.showText(context.getMessage("command.checknotes.hover.check")))
                .clickEvent(SpongeComponents.executeCallback(commandSource -> {
                    context.sendMessage("command.checknotes.id", String.valueOf(id));
                    context.sendMessage( "command.checknotes.date", date);
                    context.sendMessage( "command.checknotes.noter", noter);
                    context.sendMessage( "command.checknotes.note", nodeMessage);
                    commandSource.sendMessage(Identity.nil(), actions);
                }));

        //Create the note message
        return LinearComponents.linear(
                information.color(NamedTextColor.GREEN),
                Component.text(": "),
                Component.text(note.getNote(), NamedTextColor.YELLOW)
        );
    }
}
