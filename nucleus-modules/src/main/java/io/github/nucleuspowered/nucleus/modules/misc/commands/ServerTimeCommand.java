/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.misc.commands;

import io.github.nucleuspowered.nucleus.modules.misc.MiscPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

@NonnullByDefault
@Command(aliases = { "servertime", "realtime" }, basePermission = MiscPermissions.BASE_SERVERTIME, commandDescriptionKey = "servertime")
public class ServerTimeCommand implements ICommandExecutor<CommandSource> {

    private static DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        context.sendMessage("command.servertime.time", dtf.format(LocalDateTime.now()));
        return context.successResult();
    }
}
