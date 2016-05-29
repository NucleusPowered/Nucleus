/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.commands.signs;

import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.CommandBase;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.Player;

@Permissions(root = "warpsign")
@RegisterCommand(value = {"del", "remove", "rm"}, subcommandOf = WarpSignCommand.class)
public class RemoveWarpSignCommand extends CommandBase<Player> {
    @Override
    public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        return null;
    }
}
