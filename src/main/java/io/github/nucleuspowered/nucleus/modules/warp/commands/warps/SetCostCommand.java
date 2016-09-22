/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.commands.warps;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.argumentparsers.WarpArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import io.github.nucleuspowered.nucleus.modules.warp.config.WarpConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.warp.handlers.WarpHandler;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;

@RunAsync
@NoCost
@NoCooldown
@NoWarmup
@Permissions(root = "warp")
@RegisterCommand(value = {"cost", "setcost"}, subcommandOf = WarpCommand.class)
public class SetCostCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<CommandSource> {

    @Inject private WarpConfigAdapter warpConfigAdapter;
    @Inject private WarpHandler warpHandler;
    private final String warpKey = "warp";
    private final String costKey = "cost";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.onlyOne(new WarpArgument(Text.of(warpKey), warpConfigAdapter, false)),
            GenericArguments.onlyOne(GenericArguments.integer(Text.of(costKey)))
        };
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        WarpArgument.Result warpData = args.<WarpArgument.Result>getOne(warpKey).get();
        int cost = args.<Integer>getOne(costKey).get();
        if (cost < -1) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.warp.costset.arg"));
            return CommandResult.empty();
        }

        if (cost == -1 && warpHandler.setWarpCost(warpData.warp, -1)) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.warp.costset.reset", warpData.warp, String.valueOf(warpConfigAdapter.getNodeOrDefault().getDefaultWarpCost())));
            return CommandResult.success();
        } else if (warpHandler.setWarpCost(warpData.warp, cost)) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.warp.costset.success", warpData.warp, String.valueOf(cost)));
            return CommandResult.success();
        }

        src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.warp.costset.failed", warpData.warp));
        return CommandResult.empty();
    }
}
