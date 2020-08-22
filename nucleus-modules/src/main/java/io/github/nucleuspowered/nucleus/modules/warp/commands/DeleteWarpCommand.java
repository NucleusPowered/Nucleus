/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.commands;

import io.github.nucleuspowered.nucleus.api.module.warp.NucleusWarpService;
import io.github.nucleuspowered.nucleus.api.module.warp.data.Warp;
import io.github.nucleuspowered.nucleus.modules.warp.WarpPermissions;
import io.github.nucleuspowered.nucleus.modules.warp.event.DeleteWarpEvent;
import io.github.nucleuspowered.nucleus.modules.warp.services.WarpService;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@EssentialsEquivalent({"delwarp", "remwarp", "rmwarp"})
@NonnullByDefault
@Command(
        aliases = {"delete", "del", "#delwarp", "#remwarp", "#rmwarp"},
        basePermission = WarpPermissions.BASE_WARP_LIST,
        commandDescriptionKey = "warp.list",
        async = true,
        parentCommand = WarpCommand.class
)
public class DeleteWarpCommand implements ICommandExecutor<CommandSource> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                serviceCollection.getServiceUnchecked(WarpService.class).warpElement(false)
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Warp warp = context.requireOne(WarpService.WARP_KEY, Warp.class);
        NucleusWarpService qs = Sponge.getServiceManager().provideUnchecked(NucleusWarpService.class);

        final Cause cause;
        if (context.getCause().root() == context.getCommandSource()) {
            cause = context.getCause();
        } else {
            cause = context.getCause().with(context.getCommandSource());
        }

        DeleteWarpEvent event = new DeleteWarpEvent(cause, warp);
        if (Sponge.getEventManager().post(event)) {
            return event.getCancelMessage().map(context::errorResultLiteral)
                    .orElseGet(() -> context.errorResult("nucleus.eventcancelled"));
        }

        if (qs.removeWarp(warp.getName())) {
            // Worked. Tell them.
            context.sendMessage("command.warps.del", warp.getName());
            return context.successResult();
        }

        // Didn't work. Tell them.
        return context.errorResult("command.warps.delerror");
    }

}
