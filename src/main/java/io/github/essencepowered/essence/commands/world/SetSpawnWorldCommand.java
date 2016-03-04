/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.commands.world;

import io.github.essencepowered.essence.Util;
import io.github.essencepowered.essence.internal.CommandBase;
import io.github.essencepowered.essence.internal.annotations.Permissions;
import io.github.essencepowered.essence.internal.annotations.RegisterCommand;
import io.github.essencepowered.essence.internal.permissions.SuggestedLevel;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

/**
 * Sets spawn of world.
 *
 * Command Usage: /world setspawn Permission: essence.world.setspawn.base
 */
@Permissions(root = "world", suggestedLevel = SuggestedLevel.ADMIN)
@RegisterCommand(value = {"setspawn"}, subcommandOf = WorldCommand.class)
public class SetSpawnWorldCommand extends CommandBase<Player> {

    @Override
    public CommandSpec createSpec() {
        return CommandSpec.builder().description(Text.of("Set World Spawn Command")).executor(this).build();
    }

    @Override
    public CommandResult executeCommand(Player pl, CommandContext args) throws Exception {
        pl.getWorld().getProperties().setSpawnPosition(pl.getLocation().getBlockPosition());
        pl.sendMessage(Text.of(TextColors.GREEN, Util.getMessageWithFormat("command.world.setspawn.success")));
        return CommandResult.success();
    }
}
