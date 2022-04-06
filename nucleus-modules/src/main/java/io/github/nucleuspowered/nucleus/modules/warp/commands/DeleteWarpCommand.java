/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.commands;

import io.github.nucleuspowered.nucleus.api.module.warp.data.Warp;
import io.github.nucleuspowered.nucleus.modules.warp.WarpPermissions;
import io.github.nucleuspowered.nucleus.modules.warp.event.DeleteWarpEvent;
import io.github.nucleuspowered.nucleus.modules.warp.services.WarpService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;

@EssentialsEquivalent({"delwarp", "remwarp", "rmwarp"})
@Command(
        aliases = {"delete", "del", "#delwarp", "#remwarp", "#rmwarp"},
        basePermission = WarpPermissions.BASE_WARP_DELETE,
        commandDescriptionKey = "warp.list",
        parentCommand = WarpCommand.class
)
public class DeleteWarpCommand implements ICommandExecutor {

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                serviceCollection.getServiceUnchecked(WarpService.class).warpElement(false)
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final WarpService qs = context.getServiceCollection().getServiceUnchecked(WarpService.class);
        final Warp warp = context.requireOne(qs.warpElement(false));

        final DeleteWarpEvent event = new DeleteWarpEvent(Sponge.server().causeStackManager().currentCause(), warp);
        if (Sponge.eventManager().post(event)) {
            return event.getCancelMessage().map(context::errorResultLiteral)
                    .orElseGet(() -> context.errorResult("nucleus.eventcancelled"));
        }

        if (qs.removeWarp(warp.getNamedLocation().getName())) {
            // Worked. Tell them.
            context.sendMessage("command.warps.del", warp.getNamedLocation().getName());
            return context.successResult();
        }

        // Didn't work. Tell them.
        return context.errorResult("command.warps.delerror");
    }

}
