/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands.border;

import io.github.nucleuspowered.nucleus.argumentparsers.NucleusWorldPropertiesArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCooldown;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCost;
import io.github.nucleuspowered.nucleus.internal.annotations.NoWarmup;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.modules.world.WorldHelper;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Optional;

import javax.inject.Inject;

@NoCooldown
@NoCost
@NoWarmup
@Permissions(prefix = "world.border", mainOverride = "gen")
@RegisterCommand(value = "cancelgen", subcommandOf = BorderCommand.class)
public class CancelChunkGenCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<CommandSource> {

    private final String worldKey = "world";

    @Inject
    private WorldHelper worldHelper;

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.optional(GenericArguments.onlyOne(new NucleusWorldPropertiesArgument(Text.of(worldKey), NucleusWorldPropertiesArgument.Type.ENABLED_ONLY)))
        };
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        WorldProperties wp = getWorldFromUserOrArgs(src, worldKey, args);
        if (worldHelper.cancelPregenRunningForWorld(wp.getUniqueId())) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.world.cancelgen.cancelled", wp.getWorldName()));
            return CommandResult.success();
        }

        src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.world.cancelgen.notask", wp.getWorldName()));
        return CommandResult.empty();
    }
}
