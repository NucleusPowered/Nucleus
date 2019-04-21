/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jump.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportScanners;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.core.services.SafeTeleportService;
import io.github.nucleuspowered.nucleus.modules.jump.JumpModule;
import io.github.nucleuspowered.nucleus.modules.jump.config.JumpConfig;
import io.github.nucleuspowered.nucleus.modules.jump.config.JumpConfigAdapter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.teleport.TeleportHelperFilters;

import java.util.Optional;
import java.util.function.Function;

@Permissions(suggestedLevel = SuggestedLevel.ADMIN)
@NonnullByDefault
@RegisterCommand("unstuck")
public class UnstuckCommand extends AbstractCommand<Player> implements Reloadable {

    private int radius = 1;
    private int height = 1;

    @Override
    protected CommandResult executeCommand(Player src, CommandContext args, Cause cause) throws Exception {
        // Get the player location, find a safe location. Prevent players trying this to get out of sticky situations, height and width is 1.
        Location<World> location = Sponge.getGame().getTeleportHelper().getSafeLocation(src.getLocation(), this.height, this.radius)
            .orElseThrow(() -> ReturnMessageException.fromKey("command.unstuck.nolocation"));
        if (location.getBlockPosition().equals(src.getLocation().getBlockPosition())) {
            throw ReturnMessageException.fromKey("command.unstuck.notneeded");
        }

        if (getServiceUnchecked(SafeTeleportService.class).teleportPlayer(
                src,
                location,
                false,
                TeleportScanners.NO_SCAN,
                TeleportHelperFilters.DEFAULT).isSuccessful())
        if (SafeTeleportService.setLocation(src, location)) {
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithTextFormat("command.unstuck.success"));
            return CommandResult.success();
        }

        throw ReturnMessageException.fromKey("command.unstuck.cancelled");
    }

    @Override
    public void onReload() {
        Optional<JumpConfig> c = Nucleus.getNucleus().getConfigValue(JumpModule.ID, JumpConfigAdapter.class, Function.identity());
        this.radius = c.map(JumpConfig::getMaxUnstuckRadius).orElse(1);
        this.height = c.map(JumpConfig::getMaxUnstuckHeight).orElse(1);
    }

}
