/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jump.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportScanners;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.modules.core.services.SafeTeleportService;
import io.github.nucleuspowered.nucleus.modules.jump.config.JumpConfigAdapter;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.data.property.block.PassableProperty;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

import javax.annotation.Nullable;

@Permissions
@RegisterCommand({"jump", "j", "jmp"})
@EssentialsEquivalent({"jump", "j", "jumpto"})
@NonnullByDefault
public class JumpCommand extends AbstractCommand<Player> implements Reloadable {

    private int maxJump = 20;

    // Original code taken from EssentialCmds. With thanks to 12AwsomeMan34 for
    // the initial contribution.
    @Override
    public CommandResult executeCommand(Player player, CommandContext args, Cause cause) throws Exception {
        BlockRay<World> playerBlockRay = BlockRay.from(player).distanceLimit(this.maxJump).build();

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
            throw ReturnMessageException.fromKey("command.jump.noblock");
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
            throw ReturnMessageException.fromKey("command.jump.outsideborder");
        }

        boolean result = getServiceUnchecked(SafeTeleportService.class)
                .teleportPlayerSmart(
                        player,
                        finalLocation,
                        false,
                        true,
                        TeleportScanners.NO_SCAN
                ).isSuccessful();
        if (result) {
            player.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.jump.success"));
            return CommandResult.success();
        }

        throw ReturnMessageException.fromKey("command.jump.notsafe");
    }

    private boolean getFromBoxed(@Nullable Boolean bool) {
        return bool != null ? bool : false;
    }

    @Override public void onReload() {
        this.maxJump = Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(JumpConfigAdapter.class).getNodeOrDefault().getMaxJump();
    }
}
