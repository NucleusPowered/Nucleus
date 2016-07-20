/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.data.NoteData;
import io.github.nucleuspowered.nucleus.argumentparsers.NoteArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import io.github.nucleuspowered.nucleus.internal.command.CommandBase;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.note.handlers.NoteHandler;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;

import java.util.List;

@Permissions(suggestedLevel = SuggestedLevel.MOD)
@RunAsync
@NoWarmup
@NoCooldown
@NoCost
@RegisterCommand({"removenote", "deletenote", "delnote"})
public class RemoveNoteCommand extends CommandBase<CommandSource> {

    @Inject private NoteHandler handler;
    private final String noteKey = "note";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {GenericArguments.onlyOne(new NoteArgument(Text.of(noteKey), plugin, handler))};
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        NoteArgument.Result result = args.<NoteArgument.Result>getOne(noteKey).get();
        User user = result.user;

        List<NoteData> notes = handler.getNotes(user);
        if (notes.isEmpty()) {
            src.sendMessage(Util.getTextMessageWithFormat("command.checkwarnings.none", user.getName()));
            return CommandResult.success();
        }

        if (handler.removeNote(user, result.noteData)) {
            src.sendMessage(Util.getTextMessageWithFormat("command.removenote.success", user.getName()));
            return CommandResult.success();
        }

        src.sendMessage(Util.getTextMessageWithFormat("command.removenote.failure", user.getName()));
        return CommandResult.empty();
    }
}
