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
import io.github.nucleuspowered.nucleus.modules.freezeplayer.FreezePlayerKeys;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.storage.dataobjects.keyed.IKeyedDataObject;
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
        IUserDataObject x = getOrCreateUserOnThread(pl.getUniqueId());
        try (IKeyedDataObject.Value<Boolean> v = x.getAndSet(FreezePlayerKeys.FREEZE_PLAYER)) {
            boolean res = v.getValue().orElse(false);
            v.setValue(args.<Boolean>getOne(NucleusParameters.Keys.BOOL).orElse(!res));
            getMessageFor(src,
                    res ? "command.freezeplayer.success.frozen" : "command.freezeplayer.success.unfrozen",
                    Nucleus.getNucleus().getNameUtil().getName(pl));

        }
        return CommandResult.success();
    }
}
