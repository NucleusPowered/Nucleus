/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Warp;
import io.github.nucleuspowered.nucleus.argumentparsers.PositiveDoubleArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.modules.warp.WarpParameters;
import io.github.nucleuspowered.nucleus.modules.warp.config.WarpConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.warp.services.WarpService;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@RunAsync
@NoModifiers
@NonnullByDefault
@Permissions(prefix = "warp")
@RegisterCommand(value = {"cost", "setcost"}, subcommandOf = WarpCommand.class)
public class SetCostCommand extends AbstractCommand<CommandSource> implements Reloadable {

    private final WarpService warpService = getServiceUnchecked(WarpService.class);
    private final String costKey = "cost";
    private double defaultCost = 0;

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                WarpParameters.WARP_NO_PERM,
                GenericArguments.onlyOne(new PositiveDoubleArgument(Text.of(this.costKey)))
        };
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) {
        Warp warpData = args.<Warp>getOne(WarpParameters.WARP_KEY).get();
        double cost = args.<Double>getOne(this.costKey).get();
        if (cost < -1) {
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warp.costset.arg"));
            return CommandResult.empty();
        }

        if (cost == -1 && this.warpService.setWarpCost(warpData.getName(), -1)) {
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warp.costset.reset", warpData.getName(), String.valueOf(this.defaultCost)));
            return CommandResult.success();
        } else if (this.warpService.setWarpCost(warpData.getName(), cost)) {
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warp.costset.success", warpData.getName(), String.valueOf(cost)));
            return CommandResult.success();
        }

        src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warp.costset.failed", warpData.getName()));
        return CommandResult.empty();
    }

    @Override public void onReload() {
        this.defaultCost = getServiceUnchecked(WarpConfigAdapter.class).getNodeOrDefault().getDefaultWarpCost();
    }
}
