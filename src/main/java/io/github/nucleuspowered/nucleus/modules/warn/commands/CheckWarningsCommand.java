/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warn.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.data.WarnData;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import io.github.nucleuspowered.nucleus.internal.command.CommandBase;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.warn.handlers.WarnHandler;
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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Checks the warnings of a player.
 *
 * Command Usage: /checkwarnings user Permission: quickstart.checkwarnings.base
 */
@Permissions(suggestedLevel = SuggestedLevel.MOD)
@RunAsync
@NoWarmup
@NoCooldown
@NoCost
@RegisterCommand({"checkwarnings", "checkwarn", "warnings"})
public class CheckWarningsCommand extends CommandBase<CommandSource> {

    @Inject private WarnHandler handler;
    private final String playerKey = "player";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {GenericArguments.flags().flag("-all", "a").flag("-expired", "e").buildWith(
                GenericArguments.onlyOne(GenericArguments.user(Text.of(playerKey))))};
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        User user = args.<User>getOne(playerKey).get();

        List<WarnData> warnings;
        if (args.hasAny("all")) {
            warnings = handler.getWarnings(user);
        } else if (args.hasAny("expired")) {
            warnings = handler.getWarnings(user, false, true);
        } else {
            warnings = handler.getWarnings(user, true, false);
        }

        if (warnings.isEmpty()) {
            src.sendMessage(Util.getTextMessageWithFormat("command.checkwarnings.none", user.getName()));
            return CommandResult.success();
        }
        handler.updateWarnings(user);

        List<Text> messages = warnings.stream().sorted((a, b) -> a.getDate().compareTo(b.getDate())).map(x -> createMessage(x, user)).collect(Collectors.toList());
        messages.add(0, Util.getTextMessageWithFormat("command.checkwarnings.info"));

        PaginationService paginationService = Sponge.getGame().getServiceManager().provideUnchecked(PaginationService.class);
        paginationService.builder()
                .title(
                        Text.builder()
                                .color(TextColors.GOLD)
                                .append(Text.of(Util.getMessageWithFormat("command.checkwarnings.header", user.getName())))
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

    private Text createMessage(WarnData warning, User user) {
        String name;
        if (warning.getWarner().equals(Util.consoleFakeUUID)) {
            name = Sponge.getServer().getConsole().getName();
        } else {
            Optional<User> ou = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(warning.getWarner());
            name = ou.isPresent() ? ou.get().getName() : Util.getMessageWithFormat("standard.unknown");
        }

        //Get the remaining length of the warning
        String time;
        if (warning.getEndTimestamp().isPresent()) {
            time = Util.getTimeStringFromSeconds(Instant.now().until(warning.getEndTimestamp().get(), ChronoUnit.SECONDS));
        } else if (warning.getTimeFromNextLogin().isPresent()) {
            time = Util.getTimeStringFromSeconds(warning.getTimeFromNextLogin().get().getSeconds());
        } else {
            time = Util.getMessageWithFormat("standard.restoftime");
        }

        //Get the ID of the warning, its index in the users List<WarnData>
        int id = handler.getWarnings(user).indexOf(warning);

        //Action buttons, for a non expired warning this should look like 'Action > [Delete] - [Expire] - [Return] <'
        Text.Builder actions = Util.getTextMessageWithFormat("command.checkwarnings.action").toBuilder();

        //Add separation between the word 'Action' and action buttons
        actions.append(Text.of(TextColors.GOLD, " > "));

        //Add the delete button [Delete]
        actions.append(Text.builder().append(Text.of(TextColors.RED, Util.getMessageWithFormat("standard.action.delete")))
                .onHover(TextActions.showText(Util.getTextMessageWithFormat("command.checkwarnings.hover.delete")))
                .onClick(TextActions.executeCallback(commandSource1 -> {
                    handler.removeWarning(user, warning, true);
                    commandSource1.sendMessage(Util.getTextMessageWithFormat("command.removewarning.remove", user.getName()));
                })).build());

        //Add a - to separate it from the next action button
        actions.append(Text.of(TextColors.GOLD, " - "));

        //Add the expire button if the warning isn't expired [Expire]
        if (!warning.isExpired()) {
            actions.append(Text.builder().append(Text.of(TextColors.YELLOW, Util.getMessageWithFormat("standard.action.expire")))
                    .onHover(TextActions.showText(Util.getTextMessageWithFormat("command.checkwarnings.hover.expire")))
                    .onClick(TextActions.executeCallback(commandSource1 -> {
                        handler.removeWarning(user, warning, false);
                        commandSource1.sendMessage(Util.getTextMessageWithFormat("command.removewarning.expire", user.getName()));
                    })).build());

            //Add a - to separate it from the next action button
            actions.append(Text.of(TextColors.GOLD, " - "));
        }

        //Add the return button [Return]
        actions.append(Text.builder().append(Text.of(TextColors.GREEN, Util.getMessageWithFormat("standard.action.return")))
                .onHover(TextActions.showText(Util.getTextMessageWithFormat("command.checkwarnings.hover.return")))
                .onClick(TextActions.executeCallback(commandSource1 -> Sponge.getCommandManager().process(commandSource1, "checkwarnings " + user.getName())))
                .build());

        //Add a < to end the actions button list
        actions.append(Text.of(TextColors.GOLD, " < "));

        //Get and format the date of the warning
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.systemDefault());
        String date = dtf.format(warning.getDate());

        //Create a clickable name providing more information about the warning
        Text.Builder information = Text.builder(name)
                .onHover(TextActions.showText(Util.getTextMessageWithFormat("command.checkwarnings.hover.check")))
                .onClick(TextActions.executeCallback(commandSource -> {
                    commandSource.sendMessage(Util.getTextMessageWithFormat("command.checkwarnings.id", String.valueOf(id)));
                    commandSource.sendMessage(Util.getTextMessageWithFormat("command.checkwarnings.date", date));
                    commandSource.sendMessage(Util.getTextMessageWithFormat("command.checkwarnings.remaining", time));
                    commandSource.sendMessage(Util.getTextMessageWithFormat("command.checkwarnings.warner", name));
                    commandSource.sendMessage(Util.getTextMessageWithFormat("command.checkwarnings.warning", warning.getReason()));
                    commandSource.sendMessage(actions.build());
                }));

        //Create the warning message
        Text.Builder message = Text.builder()
                .append(Text.of(TextColors.GREEN, information.build()))
                .append(Text.of(": "))
                .append(Text.of(TextColors.YELLOW, warning.getReason()));

        //Add the remaining length of the warning
        if (warning.isExpired()) {
            message.append(Text.of(TextColors.GRAY, " " + Util.getMessageWithFormat("standard.status.expired")));
        } else {
            message.append(Text.of(TextColors.GREEN, " " + Util.getMessageWithFormat("standard.for") + " "));
            message.append(Text.of(TextColors.YELLOW, time));
        }

        return message.build();
    }
}
