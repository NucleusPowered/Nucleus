/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note.commands;

import io.github.nucleuspowered.nucleus.modules.note.NotePermissions;
import io.github.nucleuspowered.nucleus.modules.note.data.NoteData;
import io.github.nucleuspowered.nucleus.modules.note.services.NoteHandler;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.List;

@NonnullByDefault
@Command(
        aliases = {"clearnotes", "removeallnotes"},
        basePermission = NotePermissions.BASE_CLEARNOTES,
        commandDescriptionKey = "clearnotes",
        async = true
)
public class ClearNotesCommand implements ICommandExecutor<CommandSource> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.ONE_USER.get(serviceCollection)
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        User user = context.requireOne(NucleusParameters.Keys.USER, User.class);
        NoteHandler handler = context.getServiceCollection().getServiceUnchecked(NoteHandler.class);

        List<NoteData> notes = handler.getNotesInternal(user);
        if (notes.isEmpty()) {
            context.sendMessage("command.checknotes.none", user.getName());
            return context.successResult();
        }

        if (handler.clearNotes(user)) {
            context.sendMessage("command.clearnotes.success", user.getName());
            return context.successResult();
        }

        return context.errorResult("command.clearnotes.failure", user.getName());
    }
}