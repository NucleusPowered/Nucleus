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
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
@EssentialsEquivalent(value = { "ping", "pong", "echo" }, isExact = false, notes = "Returns your latency, not your message.")
@Command(
        aliases = { "ping" },
        basePermission = MiscPermissions.BASE_PING,
        commandDescriptionKey = "ping",
        associatedPermissions = MiscPermissions.OTHERS_PING
)
public class PingCommand implements ICommandExecutor<CommandSource> { // extends AbstractCommand.SimpleTargetOtherPlayer {

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                serviceCollection.commandElementSupplier()
                        .createOnlyOtherUserPermissionElement(true, MiscPermissions.OTHERS_PING)
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Player player = context.getPlayerFromArgs();
        if (context.is(player)) {
            context.sendMessage("command.ping.current.self", player.getConnection().getLatency());
        } else {
            context.sendMessage("command.ping.current.other", player.getName(), player.getConnection().getLatency());
        }

        return context.successResult();
    }
}
