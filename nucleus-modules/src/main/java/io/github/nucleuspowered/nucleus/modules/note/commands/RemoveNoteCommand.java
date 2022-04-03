/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note.commands;

import io.github.nucleuspowered.nucleus.api.module.note.data.Note;
import io.github.nucleuspowered.nucleus.modules.note.NotePermissions;
import io.github.nucleuspowered.nucleus.modules.note.services.NoteHandler;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.standard.VariableValueParameters;
import org.spongepowered.api.util.Identifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Command(
        aliases = {"removenote", "deletenote", "delnote"},
        basePermission = NotePermissions.BASE_REMOVENOTE,
        commandDescriptionKey = "removenote")
public class RemoveNoteCommand implements ICommandExecutor {

    private final Parameter.Value<Integer> index = Parameter.builder(Integer.class)
            .key("index")
            .addParser(VariableValueParameters.integerRange().min(1).build())
            .build();

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.Composite.USER_OR_GAME_PROFILE,
                this.index
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final NoteHandler handler = context.getServiceCollection().getServiceUnchecked(NoteHandler.class);
        final UUID uuid = NucleusParameters.Composite.parseUserOrGameProfile(context)
                .fold(Function.identity(), Identifiable::uniqueId);
        final Component name = context.getDisplayName(uuid);
        final int idx = context.requireOne(this.index);

        handler.getNotes(uuid).thenAccept(x -> {
            if (x.isEmpty()) {
                context.sendMessage("command.checknotes.none", name);
            } else if (x.size() > idx) {
                context.sendMessage("args.note.nonotedata", idx, name);
            } else {
                final List<Note> list = new ArrayList<>(x);
                final Note note = list.get(idx);
                if (handler.removeNote(uuid, note).join()) {
                    context.sendMessage("command.removenote.success", name);
                } else {
                    context.sendMessage("command.removenote.failure", name);
                }
            }
        });
        return context.successResult();
    }
}
