/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.core.services.SafeTeleportService;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.util.annotation.NonnullByDefault;

/**
 * Teleports you to the world spawn.
 *
 * Command Usage: /world spawn Permission: nucleus.world.spawn.base
 */
@Permissions(prefix = "world", suggestedLevel = SuggestedLevel.ADMIN)
@RegisterCommand(value = {"spawn"}, subcommandOf = WorldCommand.class)
@NonnullByDefault
public class WorldSpawnCommand extends AbstractCommand<Player> {

    @Override
    public CommandResult executeCommand(Player pl, CommandContext args, Cause cause) {
        SafeTeleportService.setLocation(pl, pl.getWorld().getSpawnLocation());
        pl.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.world.spawn.success"));
        return CommandResult.success();
    }
}
