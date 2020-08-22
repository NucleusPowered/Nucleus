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
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.storage.WorldProperties;

@NonnullByDefault
@Command(
        aliases = {"rename"},
        basePermission = WorldPermissions.BASE_WORLD_RENAME,
        commandDescriptionKey = "world.rename",
        parentCommand = WorldCommand.class
)
public class RenameWorldCommand implements ICommandExecutor<CommandSource> {

    private final String newNameKey = "new name";

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.WORLD_PROPERTIES_UNLOADED_ONLY.get(serviceCollection),
                GenericArguments.string(Text.of(this.newNameKey))
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        WorldProperties worldProperties = context.requireOne(NucleusParameters.Keys.WORLD, WorldProperties.class);
        String oldName = worldProperties.getWorldName();
        String newName =  context.requireOne(this.newNameKey, String.class);
        if (Sponge.getServer().renameWorld(worldProperties, newName).isPresent()) {
            context.sendMessage("command.world.rename.success", oldName, newName);
            return context.successResult();
        }

        return context.errorResult("command.world.rename.failed", worldProperties.getWorldName(), newName);
    }
}
