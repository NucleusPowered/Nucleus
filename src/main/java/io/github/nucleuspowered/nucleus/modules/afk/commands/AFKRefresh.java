/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.afk.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.modules.afk.handlers.AFKHandler;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;

@Permissions
@RegisterCommand("afkrefresh")
public class AFKRefresh extends AbstractCommand<CommandSource> {

    @Inject private AFKHandler handler;

    @Override public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        handler.invalidateAfkCache();
        src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.afkrefresh.complete"));
        return CommandResult.success();
    }
}
