/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands;

import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.ArrayList;
import java.util.List;

@Command(
        aliases = {"info"},
        basePermission = WorldPermissions.BASE_WORLD_INFO,
        commandDescriptionKey = "world.info",
        parentCommand = WorldCommand.class
)
public class InfoWorldCommand implements ICommandExecutor {

    @Override public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.ONLINE_WORLD_OPTIONAL
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final ServerWorld wp = context.getWorldPropertiesOrFromSelf(NucleusParameters.ONLINE_WORLD_OPTIONAL);
        final List<Component> listContent = new ArrayList<>();
        final boolean canSeeSeeds = context.testPermission(WorldPermissions.WORLD_SEED);
        ListWorldCommand.getWorldInfo(context, listContent, wp, canSeeSeeds);
        Util.getPaginationBuilder(context.audience())
                .contents(listContent)
                .title(context.getMessage("command.world.info.title", wp.key().asString()))
                .sendTo(context.audience());

        return context.successResult();
    }
}
