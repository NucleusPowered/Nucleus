/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands;

import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.CommandFlags;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Optional;

@NonnullByDefault
@Command(
        aliases = {"load"},
        basePermission = WorldPermissions.BASE_WORLD_LOAD,
        commandDescriptionKey = "world.load",
        parentCommand = WorldCommand.class
)
public class LoadWorldCommand implements ICommandExecutor<CommandSource> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
            GenericArguments.flags()
                    .permissionFlag(WorldPermissions.BASE_WORLD_ENABLE, "e", "-enable")
                .setUnknownShortFlagBehavior(CommandFlags.UnknownFlagBehavior.IGNORE)
                .buildWith(NucleusParameters.WORLD_PROPERTIES_ENABLED_ONLY.get(serviceCollection))
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        WorldProperties worldProperties = context.requireOne(NucleusParameters.Keys.WORLD, WorldProperties.class);
        if (!worldProperties.isEnabled() && !context.hasAny("e")) {
            // Not enabled, cannot load.
            if (context.testPermission(WorldPermissions.BASE_WORLD_ENABLE)) {
                return context.errorResult("command.world.load.notenabled.enable", worldProperties.getWorldName());
            }

            return context.errorResult("command.world.load.notenabled.noenable", worldProperties.getWorldName());
        }

        if (Sponge.getServer().getWorld(worldProperties.getUniqueId()).isPresent()) {
            return context.errorResult("command.world.load.alreadyloaded", worldProperties.getWorldName());
        }

        worldProperties.setEnabled(true);
        context.sendMessage("command.world.load.start", worldProperties.getWorldName());
        Optional<World> optional = Sponge.getServer().loadWorld(worldProperties);
        if (optional.isPresent()) {
            context.sendMessage("command.world.load.loaded", worldProperties.getWorldName());
            return context.successResult();
        }

        return context.errorResult("command.world.load.fail", worldProperties.getWorldName());
    }


}
