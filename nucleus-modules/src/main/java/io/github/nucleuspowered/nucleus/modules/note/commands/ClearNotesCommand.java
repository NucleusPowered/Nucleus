/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note.commands;

import io.github.nucleuspowered.nucleus.modules.note.NotePermissions;
import io.github.nucleuspowered.nucleus.modules.note.services.NoteService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ISchedulerService;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.util.Identifiable;

import java.util.UUID;
import java.util.function.Function;

@Command(
        aliases = {"clearnotes", "removeallnotes"},
        basePermission = NotePermissions.BASE_CLEARNOTES,
        commandDescriptionKey = "clearnotes")
public class ClearNotesCommand implements ICommandExecutor {

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.Composite.USER_OR_GAME_PROFILE
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final UUID user = NucleusParameters.Composite.parseUserOrGameProfile(context).fold(Function.identity(), Identifiable::uniqueId);
        final Component name = context.getDisplayName(user);
        final NoteService handler = context.getServiceCollection().getServiceUnchecked(NoteService.class);

        handler.getNotes(user).thenAccept(x -> {
            final ISchedulerService service = context.getServiceCollection().schedulerService();
            if (x.isEmpty()) {
                service.runOnMainThread(() -> context.sendMessage("command.checknotes.none", name));
            } else if (handler.clearNotes(user).join()) {
                service.runOnMainThread(() -> context.sendMessage("command.clearnotes.success", name));
            } else {
                service.runOnMainThread(() -> context.sendMessage("command.clearnotes.failure", name));
            }
        });
        return context.successResult();
    }
}