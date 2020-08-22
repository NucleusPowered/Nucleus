/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.commands;

import com.flowpowered.math.vector.Vector3d;
import io.github.nucleuspowered.nucleus.modules.spawn.SpawnKeys;
import io.github.nucleuspowered.nucleus.modules.spawn.SpawnPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
@EssentialsEquivalent("setspawn")
@Command(
        aliases = { "setspawn" },
        basePermission = SpawnPermissions.BASE_SETSPAWN,
        commandDescriptionKey = "setspawn"
)
public class SetSpawnCommand implements ICommandExecutor<Player> {

    @Override public ICommandResult execute(ICommandContext<? extends Player> context) throws CommandException {
        Player src = context.getIfPlayer();
        // Minecraft does not set the rotation of the player at the spawn point, so we'll do it for them!
        final Vector3d rotation = src.getRotation();
        context.getServiceCollection()
                .storageManager()
                .getWorldService()
                .getOrNew(src.getUniqueId())
                .thenAccept(x -> x.set(SpawnKeys.WORLD_SPAWN_ROTATION, rotation));

        src.getWorld().getProperties().setSpawnPosition(src.getLocation().getBlockPosition());
        context.sendMessage("command.setspawn.success", src.getWorld().getName());
        return context.successResult();
    }
}
