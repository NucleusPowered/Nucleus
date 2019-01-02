/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.freezeplayer.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.modules.freezeplayer.services.FreezePlayerService;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@Permissions(supportsOthers = true)
@RegisterCommand({"freezeplayer", "freeze"})
@NonnullByDefault
@RunAsync
public class FreezePlayerCommand extends AbstractCommand<CommandSource> {

    private final FreezePlayerService service =
            Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(FreezePlayerService.class);

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.optionalWeak(requirePermissionArg(
                        NucleusParameters.ONE_PLAYER, this.permissions.getPermissionWithSuffix("others"))),
                GenericArguments.optional(NucleusParameters.ONE_TRUE_FALSE)
        };
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) throws Exception {
        User pl = this.getUserFromArgs(User.class, src, NucleusParameters.Keys.PLAYER, args);
        final boolean f = args.<Boolean>getOne(NucleusParameters.Keys.BOOL).orElseGet(() -> !this.service.isFrozen(pl));
        this.service.setFrozen(pl, f);
        src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat(
            f ? "command.freezeplayer.success.frozen" : "command.freezeplayer.success.unfrozen",
                Nucleus.getNucleus().getNameUtil().getName(pl)));
        return CommandResult.success();
    }
}
