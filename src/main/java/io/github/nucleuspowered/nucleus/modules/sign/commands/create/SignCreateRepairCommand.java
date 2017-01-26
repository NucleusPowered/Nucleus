/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.sign.commands.create;

import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.modules.sign.commands.AbstractTargetSignCommand;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;

@Permissions(prefix = "sign.create")
@RegisterCommand(value = "repair")
public class SignCreateRepairCommand extends AbstractTargetSignCommand.Create {

    @Override public CommandElement[] getArguments() {
        return new CommandElement[] {
            getFlags().buildWith(GenericArguments.none())
        };
    }

    @Override protected CommandResult executeCreateCommand(Player src, CommandContext args, Sign sign) throws Exception {
        return CommandResult.success();
    }
}
