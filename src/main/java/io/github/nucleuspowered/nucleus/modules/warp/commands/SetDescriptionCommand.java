/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Warp;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.modules.warp.WarpParameters;
import io.github.nucleuspowered.nucleus.modules.warp.services.WarpService;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@RunAsync
@NoModifiers
@NonnullByDefault
@Permissions(prefix = "warp")
@RegisterCommand(value = {"setdescription"}, subcommandOf = WarpCommand.class)
public class SetDescriptionCommand extends AbstractCommand<CommandSource> {

    private final WarpService handler = getServiceUnchecked(WarpService.class);

    @Override public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.flags().flag("r", "-remove", "-delete").buildWith(
                GenericArguments.seq(
                        WarpParameters.WARP_NO_PERM,
                        NucleusParameters.OPTIONAL_DESCRIPTION
                )
            )
        };
    }

    @Override public CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) throws Exception {
        String warpName = args.<Warp>getOne(WarpParameters.WARP_KEY).get().getName();
        if (args.hasAny("r")) {
            // Remove the desc.
            if (this.handler.setWarpDescription(warpName, null)) {
                src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warp.description.removed", warpName));
                return CommandResult.success();
            }

            throw new ReturnMessageException(Nucleus
                    .getNucleus().getMessageProvider().getTextMessageWithFormat("command.warp.description.noremove", warpName));
        }

        // Add the category.
        Text message = TextSerializers.FORMATTING_CODE.deserialize(args.<String>getOne(NucleusParameters.Keys.DESCRIPTION).get());
        if (this.handler.setWarpDescription(warpName, message)) {
            src.sendMessage(
                    Nucleus.getNucleus().getMessageProvider().getTextMessageWithTextFormat("command.warp.description.added", message, Text.of(warpName)));
            return CommandResult.success();
        }

        throw new ReturnMessageException(Nucleus.getNucleus().getMessageProvider().getTextMessageWithTextFormat("command.warp.description.couldnotadd",
                Text.of(warpName)));
    }
}
