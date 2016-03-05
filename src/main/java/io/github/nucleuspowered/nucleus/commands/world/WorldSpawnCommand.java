/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.commands.world;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.internal.CommandBase;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

/**
 * Teleports you to the world spawn.
 *
 * Command Usage: /world spawn Permission: nucleus.world.spawn.base
 */
@Permissions(root = "world", suggestedLevel = SuggestedLevel.ADMIN)
@RegisterCommand(value = {"spawn"}, subcommandOf = WorldCommand.class)
public class WorldSpawnCommand extends CommandBase<Player> {

    @Override
    public CommandSpec createSpec() {
        return CommandSpec.builder().executor(this).description(Text.of("World Spawn Command")).build();
    }

    @Override
    public CommandResult executeCommand(Player pl, CommandContext args) throws Exception {
        pl.setLocation(pl.getWorld().getSpawnLocation());
        pl.sendMessage(Util.getTextMessageWithFormat("command.world.spawn.success"));
        return CommandResult.success();
    }
}
