/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.modules.spawn.SpawnKeys;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@RegisterCommand({"setfirstspawn"})
@Permissions(prefix = "firstspawn", mainOverride = "set")
@NoModifiers
@RunAsync
@NonnullByDefault
public class SetFirstSpawnCommand extends AbstractCommand<Player> {

    @Override
    public CommandResult executeCommand(Player src, CommandContext args, Cause cause) {
        Nucleus.getNucleus().getStorageManager().getGeneralService().getOrNewOnThread()
                .set(SpawnKeys.FIRST_SPAWN_LOCATION, new LocationNode(src.getLocation(), src.getRotation()));
        src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.setfirstspawn.success"));
        src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.setfirstspawn.success2"));

        return CommandResult.success();
    }

}
