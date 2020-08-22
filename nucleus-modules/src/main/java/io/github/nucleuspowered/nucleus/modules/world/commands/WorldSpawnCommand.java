/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands;

import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
@Command(
        aliases = {"spawn"},
        basePermission = WorldPermissions.BASE_WORLD_SPAWN,
        commandDescriptionKey = "world.spawn",
        parentCommand = WorldCommand.class,
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = WorldPermissions.EXEMPT_COOLDOWN_WORLD_SPAWN),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = WorldPermissions.EXEMPT_WARMUP_WORLD_SPAWN),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = WorldPermissions.EXEMPT_COST_WORLD_SPAWN)
        }
)
public class WorldSpawnCommand implements ICommandExecutor<Player> {

    @Override public ICommandResult execute(ICommandContext<? extends Player> context) throws CommandException {
        context.getServiceCollection().teleportService()
                .setLocation(context.getIfPlayer(), context.getIfPlayer().getWorld().getSpawnLocation());
        context.sendMessage("command.world.spawn.success");
        return context.successResult();
    }
}
