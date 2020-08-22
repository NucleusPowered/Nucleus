/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note.commands;

import io.github.nucleuspowered.nucleus.modules.note.NotePermissions;
import io.github.nucleuspowered.nucleus.modules.note.data.NoteData;
import io.github.nucleuspowered.nucleus.modules.note.parameter.NoteArgument;
import io.github.nucleuspowered.nucleus.modules.note.services.NoteHandler;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.List;

@NonnullByDefault
@Command(
        aliases = {"removenote", "deletenote", "delnote"},
        basePermission = NotePermissions.BASE_REMOVENOTE,
        commandDescriptionKey = "removenote",
        async = true
)
public class RemoveNoteCommand implements ICommandExecutor<CommandSource> {

    private final String noteKey = "note";

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        NoteHandler handler = serviceCollection.getServiceUnchecked(NoteHandler.class);
        IMessageProviderService messageProviderService = serviceCollection.messageProvider();
        return new CommandElement[] {
                GenericArguments.onlyOne(new NoteArgument(Text.of(this.noteKey), handler, messageProviderService))
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        NoteArgument.Result result = context.requireOne(this.noteKey, NoteArgument.Result.class);
        NoteHandler handler = context.getServiceCollection().getServiceUnchecked(NoteHandler.class);
        User user = result.user;

        List<NoteData> notes = handler.getNotesInternal(user);
        if (notes.isEmpty()) {
            context.sendMessage("command.checkwarnings.none", user.getName());
            return context.successResult();
        }

        if (handler.removeNote(user, result.noteData)) {
            context.sendMessage("command.removenote.success", user.getName());
            return context.successResult();
        }

        return context.errorResult("command.removenote.failure", user.getName());
    }
}
