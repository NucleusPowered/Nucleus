/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.back.commands;

import io.github.nucleuspowered.nucleus.argumentparsers.NucleusRequirePermissionArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.modules.back.services.BackHandler;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
@NoModifiers
@Permissions(supportsOthers = true)
@RegisterCommand("clearback")
public class ClearBackCommand extends AbstractCommand<CommandSource> {

    @Override public CommandElement[] getArguments() {
        return new CommandElement[] {
                new NucleusRequirePermissionArgument(
                        GenericArguments.optionalWeak(NucleusParameters.ONE_USER),
                        this.permissions.getOthers())
        };
    }

    @Override public CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) throws Exception {
        User target = this.getUserFromArgs(User.class, src, NucleusParameters.Keys.USER, args);
        boolean isSelf = src instanceof Player && ((Player) src).getUniqueId().equals(target.getUniqueId());
        if (!isSelf) {
            if (!hasPermission(src, this.permissions.getOthers())) {
                // no permission
                throw ReturnMessageException.fromKey("command.clearback.other.noperm");
            }
        }

        getServiceUnchecked(BackHandler.class).removeLastLocation(target);
        sendMessageTo(src, "command.clearback.success", target.getName());
        return CommandResult.success();
    }


}
