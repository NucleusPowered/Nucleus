/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jump.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.modules.jump.config.JumpConfigAdapter;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.data.property.block.PassableProperty;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

@Permissions
@RegisterCommand({"jump", "j", "jmp"})
public class JumpCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<Player> {

    @Inject private JumpConfigAdapter jca;

    // Original code taken from EssentialCmds. With thanks to 12AwsomeMan34 for
    // the initial contribution.
    @Override
    public CommandResult executeCommand(Player player, CommandContext args) throws Exception {
        BlockRay<World> playerBlockRay = BlockRay.from(player).distanceLimit(jca.getNode().getMaxJump()).build();

        BlockRayHit<World> finalHitRay = null;

        // Iterate over the blocks until we get a solid block.
        while (finalHitRay == null && playerBlockRay.hasNext()) {
            BlockRayHit<World> currentHitRay = playerBlockRay.next();
            if (!player.getWorld().getBlockType(currentHitRay.getBlockPosition()).equals(BlockTypes.AIR)) {
                finalHitRay = currentHitRay;
            }
        }

        if (finalHitRay == null) {
            // We didn't find anywhere to jump to.
            player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.jump.noblock"));
            return CommandResult.empty();
        }

        // If the block not passable, then it is a solid block
        Location<World> finalLocation = finalHitRay.getLocation();
        Optional<PassableProperty> pp = finalHitRay.getLocation().getProperty(PassableProperty.class);
        if (pp.isPresent() && !getFromBoxed(pp.get().getValue())) {
            finalLocation = finalLocation.add(0, 1, 0);
        } else {
            Optional<PassableProperty> ppbelow = finalHitRay.getLocation().getRelative(Direction.DOWN).getProperty(PassableProperty.class);
            if (ppbelow.isPresent() && !getFromBoxed(ppbelow.get().getValue())) {
                finalLocation = finalLocation.sub(0, 1, 0);
            }
        }

        if (!Util.isLocationInWorldBorder(finalLocation)) {
            player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.jump.outsideborder"));
            return CommandResult.empty();
        }

        if (player.setLocationSafely(finalLocation)) {
            player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.jump.success"));
            return CommandResult.success();
        }

        player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.jump.notsafe"));
        return CommandResult.empty();
    }

    private boolean getFromBoxed(Boolean bool) {
        return bool != null ? bool : false;
    }
}
