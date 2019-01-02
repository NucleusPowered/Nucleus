/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.commands;

import com.flowpowered.math.vector.Vector3d;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.modules.spawn.SpawnKeys;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@RegisterCommand({"setspawn"})
@Permissions
@NoModifiers
@NonnullByDefault
@EssentialsEquivalent("setspawn")
public class SetSpawnCommand extends AbstractCommand<Player> {

    @Override
    public CommandResult executeCommand(Player src, CommandContext args, Cause cause) {
        // Minecraft does not set the rotation of the player at the spawn point, so we'll do it for them!
        final Vector3d rotation = src.getRotation();
        Nucleus.getNucleus().getStorageManager().getWorldService()
                .getOrNew(src.getUniqueId())
                .thenAccept(x -> x.set(SpawnKeys.WORLD_SPAWN_ROTATION, rotation));

        src.getWorld().getProperties().setSpawnPosition(src.getLocation().getBlockPosition());
        sendMessageTo(src, "command.setspawn.success", src.getWorld().getName());
        return CommandResult.success();
    }
}
