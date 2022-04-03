/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note.commands;

import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.modules.note.NotePermissions;
import io.github.nucleuspowered.nucleus.modules.note.services.NoteHandler;
import io.github.nucleuspowered.nucleus.modules.note.services.UserNote;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import net.kyori.adventure.audience.Audience;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.User;

import java.time.Instant;
import java.util.UUID;

@Command(
        aliases = {"note", "addnote"},
        basePermission = NotePermissions.NOTE_NOTIFY,
        commandDescriptionKey = "note",
        associatedPermissions = {
                NotePermissions.NOTE_NOTIFY,
                NotePermissions.NOTE_SHOWONLOGIN
        }
)
public class NoteCommand implements ICommandExecutor {

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.ONE_USER,
                NucleusParameters.MESSAGE
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final User user = context.getUserFromArgs(NucleusParameters.ONE_USER);
        final String note = context.requireOne(NucleusParameters.MESSAGE);

        final UUID noter = context.uniqueId().orElse(Util.CONSOLE_FAKE_UUID);
        final UserNote noteData = new UserNote(noter, note, Instant.now());

        context.getServiceCollection().getServiceUnchecked(NoteHandler.class).addNote(user.uniqueId(), noteData).thenAccept(x -> {
            if (x) {
                final Audience messageChannel =
                        Audience.audience(
                                context.getServiceCollection().permissionService().permissionMessageChannel(NotePermissions.NOTE_NOTIFY),
                                context.audience());
                final IMessageProviderService messageProviderService = context.getServiceCollection().messageProvider();
                context.getServiceCollection().schedulerService().runOnMainThread(() -> {
                        messageProviderService.sendMessageTo(messageChannel, "command.note.success", context.getName(), noteData.getNote(),
                                user.name());
                });
            } else {
                context.sendMessage("command.warn.fail", user.name());
            }
        });

        return context.successResult();
    }
}
