/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.commands.signs;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.argumentparsers.PositiveIntegerArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.WarpArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.CommandBase;
import io.github.nucleuspowered.nucleus.modules.warp.config.WarpConfigAdapter;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

@Permissions(root = "warpsign")
@RegisterCommand(value = "set", subcommandOf = WarpSignCommand.class)
public class SetWarpSignCommand extends CommandBase<Player> {

    private final String warpKey = "warp";
    private final String warmupKey = "warmup";
    private final String permissionKey = "permission";

    @Inject private WarpConfigAdapter wca;

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
            // Warp name, warmup time, permission
                GenericArguments.onlyOne(new WarpArgument(Text.of(warpKey), wca, false)),
                GenericArguments.optionalWeak(GenericArguments.onlyOne(new PositiveIntegerArgument(Text.of(warmupKey)))),
                GenericArguments.optionalWeak(GenericArguments.onlyOne(GenericArguments.string(Text.of(permissionKey))))
        };
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        return null;
    }
}
