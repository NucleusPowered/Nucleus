/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportScanners;
import io.github.nucleuspowered.nucleus.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.core.services.SafeTeleportService;
import io.github.nucleuspowered.nucleus.modules.spawn.SpawnKeys;
import io.github.nucleuspowered.nucleus.modules.spawn.config.SpawnConfigAdapter;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.World;

import java.util.Optional;

@Permissions(suggestedLevel = SuggestedLevel.USER)
@RegisterCommand("firstspawn")
@NonnullByDefault
public class FirstSpawnCommand extends AbstractCommand<Player> implements Reloadable {

    private boolean isSafeTeleport = true;

    @Override
    public CommandResult executeCommand(Player src, CommandContext args, Cause cause) {

        Optional<Transform<World>> olwr = Nucleus.getNucleus().getStorageManager().getGeneralService()
                .getOrNewOnThread()
                .get(SpawnKeys.FIRST_SPAWN_LOCATION)
                .flatMap(LocationNode::getTransformIfExists);
        if (!olwr.isPresent()) {
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.firstspawn.notset"));
            return CommandResult.empty();
        }

        TeleportResult result = getServiceUnchecked(SafeTeleportService.class)
                .teleportPlayerSmart(
                        src,
                        olwr.get(),
                        true,
                        this.isSafeTeleport,
                        TeleportScanners.NO_SCAN
                );
        if (result.isSuccessful()) {
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.firstspawn.success"));
            return CommandResult.success();
        }

        src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.firstspawn.fail"));
        return CommandResult.empty();
    }


    @Override public void onReload() {
        this.isSafeTeleport = getServiceUnchecked(SpawnConfigAdapter.class).getNodeOrDefault().isSafeTeleport();
    }
}
