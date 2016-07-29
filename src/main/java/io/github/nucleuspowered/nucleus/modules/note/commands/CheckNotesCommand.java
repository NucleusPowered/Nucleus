/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note.commands;

import com.google.common.collect.Lists;
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
import org.spongepowered.api.text.format.TextColors;

import java.util.List;
import java.util.Optional;

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

        int index = 0;
        List<Text> messages = Lists.newArrayList();
        for (NoteData note : notes) {
            String name;
            if (note.getNoter().equals(Util.consoleFakeUUID)) {
                name = Sponge.getServer().getConsole().getName();
            } else {
                Optional<User> ou = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(note.getNoter());
                name = ou.isPresent() ? ou.get().getName() : Util.getMessageWithFormat("standard.unknown");
            }

            messages.add(Util.getTextMessageWithFormat("command.checknotes.note", String.valueOf(index + 1), note.getNote(), name));
            index++;
        }

        PaginationService paginationService = Sponge.getGame().getServiceManager().provideUnchecked(PaginationService.class);
        paginationService.builder()
                .title(
                        Text.builder()
                        .color(TextColors.GOLD)
                        .append(Text.of(user.getName() + Util.getMessageWithFormat("command.checknotes.header")))
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
}
