/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.commands;

import io.github.nucleuspowered.nucleus.api.module.warp.data.Warp;
import io.github.nucleuspowered.nucleus.modules.warp.WarpPermissions;
import io.github.nucleuspowered.nucleus.modules.warp.config.WarpConfig;
import io.github.nucleuspowered.nucleus.modules.warp.services.WarpService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;

@Command(
        aliases = {"cost", "setcost"},
        basePermission = WarpPermissions.BASE_WARP_COST,
        commandDescriptionKey = "warp.cost",
        parentCommand = WarpCommand.class
)
public class SetCostCommand implements ICommandExecutor, IReloadableService.Reloadable {

    private double defaultCost = 0;

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                serviceCollection.getServiceUnchecked(WarpService.class).warpElement(false),
                NucleusParameters.COST
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final WarpService warpService = context.getServiceCollection().getServiceUnchecked(WarpService.class);
        final Warp warpData = context.requireOne(warpService.warpElement(false));
        final double cost = context.requireOne(NucleusParameters.COST);
        if (cost < -1) {
            return context.errorResult("command.warp.costset.arg");
        }

        if (cost == -1 && warpService.setWarpCost(warpData.getNamedLocation().getName(), -1)) {
            context.sendMessage("command.warp.costset.reset", warpData.getNamedLocation().getName(), String.valueOf(this.defaultCost));
            return context.successResult();
        } else if (warpService.setWarpCost(warpData.getNamedLocation().getName(), cost)) {
            context.sendMessage("command.warp.costset.success", warpData.getNamedLocation().getName(), cost);
            return context.successResult();
        }

        return context.errorResult("command.warp.costset.failed", warpData.getNamedLocation().getName());
    }

    @Override public void onReload(final INucleusServiceCollection serviceCollection) {
        this.defaultCost = serviceCollection.configProvider()
                .getModuleConfig(WarpConfig.class)
                .getDefaultWarpCost();
    }

}
