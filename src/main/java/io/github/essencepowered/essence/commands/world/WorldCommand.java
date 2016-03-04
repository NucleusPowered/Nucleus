/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.commands.world;

import io.github.essencepowered.essence.api.PluginModule;
import io.github.essencepowered.essence.internal.CommandBase;
import io.github.essencepowered.essence.internal.annotations.Modules;
import io.github.essencepowered.essence.internal.annotations.Permissions;
import io.github.essencepowered.essence.internal.annotations.RegisterCommand;
import io.github.essencepowered.essence.internal.permissions.SuggestedLevel;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;

/**
 * Allows a user to warp to the specified warp.
 *
 * Command Usage: /world Permission: essence.world.base
 *
 */
@Permissions(suggestedLevel = SuggestedLevel.ADMIN)
@Modules(PluginModule.WORLDS)
@RegisterCommand("world")
public class WorldCommand extends CommandBase<CommandSource> {
    @Override
    public CommandSpec createSpec() {
        return CommandSpec.builder()
                .executor(this)
                .children(this.createChildCommands())
                .build();
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        return CommandResult.empty();
    }
}
