/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.List;

@NonnullByDefault
@Command(
        aliases = {"info"},
        basePermission = WorldPermissions.BASE_WORLD_INFO,
        commandDescriptionKey = "world.info",
        parentCommand = WorldCommand.class
)
public class InfoWorldCommand implements ICommandExecutor<CommandSource> {

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.WORLD_PROPERTIES_ALL.get(serviceCollection)
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        WorldProperties wp = context.getWorldPropertiesOrFromSelf(NucleusParameters.Keys.WORLD)
                .orElseThrow(() -> context.createException("command.world.player"));
        final List<Text> listContent = Lists.newArrayList();
        final boolean canSeeSeeds = context.testPermission(WorldPermissions.WORLD_SEED);
        ListWorldCommand.getWorldInfo(context, listContent, wp, canSeeSeeds);
        Util.getPaginationBuilder(context.getCommandSourceUnchecked())
                .contents(listContent)
                .title(context.getMessage("command.world.info.title", wp.getWorldName()))
                .sendTo(context.getCommandSourceUnchecked());

        return context.successResult();
    }
}
