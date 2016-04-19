/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mail.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import io.github.nucleuspowered.nucleus.internal.command.CommandBase;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.mail.handlers.MailHandler;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.Player;

/**
 * Permission is "quickstart.mail.base", because a player should always be able
 * to clear mail if they can read it.
 */
@Permissions(alias = "mail", suggestedLevel = SuggestedLevel.USER)
@NoWarmup
@NoCooldown
@NoCost
@RunAsync
@RegisterCommand(value = "clear", subcommandOf = MailCommand.class)
public class ClearMailCommand extends CommandBase<Player> {

    @Inject private MailHandler handler;

    @Override
    public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        if (handler.clearUserMail(src)) {
            src.sendMessage(Util.getTextMessageWithFormat("command.mail.clear.success"));
        } else {
            src.sendMessage(Util.getTextMessageWithFormat("command.mail.clear.nomail"));
        }
        return CommandResult.success();
    }
}
