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
@RegisterCommand({"clearwarnings", "removeallwarnings"})
public class ClearWarningsCommand extends CommandBase<CommandSource> {

    @Inject private WarnHandler handler;
    private final String playerKey = "player";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {GenericArguments.flags().flag("-all", "a").flag("-remove", "r").flag("-expired", "e").buildWith(
                        GenericArguments.onlyOne(GenericArguments.user(Text.of(playerKey))))};
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        User user = args.<User>getOne(playerKey).get();

        List<WarnData> warnings = handler.getWarnings(user);
        if (warnings.isEmpty()) {
            src.sendMessage(Util.getTextMessageWithFormat("command.checkwarnings.none", user.getName()));
            return CommandResult.success();
        }

        //By default expire all active warnings.
        //If the flag --all is used then remove all warnings
        //If the flag --expired is used then remove all expired warnings.
        //If the flag --remove is used then remove all active warnings.
        boolean removeActive = false;
        boolean removeExpired = false;
        Text message = Util.getTextMessageWithFormat("command.clearwarnings.success", user.getName());
        if (args.hasAny("all")) {
            removeActive = true;
            removeExpired = true;
            message = Util.getTextMessageWithFormat("command.clearwarnings.all", user.getName());
        } else if (args.hasAny("remove")) {
            removeActive = true;
            message = Util.getTextMessageWithFormat("command.clearwarnings.remove", user.getName());
        } else if (args.hasAny("expired")) {
            removeExpired = true;
            message = Util.getTextMessageWithFormat("command.clearwarnings.expired", user.getName());
        }

        if (handler.clearWarnings(user, removeActive, removeExpired)) {
            src.sendMessage(message);
            return CommandResult.success();
        }

        src.sendMessage(Util.getTextMessageWithFormat("command.clearwarnings.failure", user.getName()));
        return CommandResult.empty();
    }
}
