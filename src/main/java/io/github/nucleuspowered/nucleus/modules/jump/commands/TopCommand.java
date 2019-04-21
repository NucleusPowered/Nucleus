/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jump.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.catalogkeys.NucleusTeleportHelperFilters;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportResults;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportScanners;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.modules.core.services.SafeTeleportService;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.teleport.TeleportHelperFilters;

@Permissions(supportsSelectors = true, supportsOthers = true)
@RegisterCommand({"top", "tosurface", "totop"})
@EssentialsEquivalent("top")
@NonnullByDefault
public class TopCommand extends AbstractCommand<CommandSource> {

    @Override public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.flags().flag("f").buildWith(
                GenericArguments.optional(
                        requirePermissionArg(NucleusParameters.ONE_PLAYER, this.permissions.getOthers()))
            )
        };
    }

    @Override public CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) throws Exception {
        Player playerToTeleport = this.getUserFromArgs(Player.class, src, NucleusParameters.Keys.PLAYER, args);

        // Get the topmost block for the subject.
        Location<World> location = playerToTeleport.getLocation();
        double x = location.getX();
        double z = location.getZ();
        Location<World> start = new Location<>(location.getExtent(), x, location.getExtent().getBlockMax().getY(), z);
        BlockRayHit<World> end = BlockRay.from(start).stopFilter(BlockRay.onlyAirFilter())
            .to(playerToTeleport.getLocation().getPosition().sub(0, 1, 0)).end()
            .orElseThrow(() -> ReturnMessageException.fromKey(src, "command.top.nothingfound"));

        if (playerToTeleport.getLocation().getBlockPosition().equals(end.getBlockPosition())) {
            if (!playerToTeleport.equals(src)) {
                throw ReturnMessageException.fromKey(src,
                        "command.top.attop.other",
                        Nucleus.getNucleus().getNameUtil().getName(playerToTeleport));
            } else {
                throw ReturnMessageException.fromKey(src, "command.top.attop.self");
            }
        }

        boolean isSafe = !args.hasAny("f");
        TeleportResult result = getServiceUnchecked(SafeTeleportService.class)
                .teleportPlayer(
                        playerToTeleport,
                        end.getLocation(),
                        playerToTeleport.getRotation(),
                        false,
                        TeleportScanners.NO_SCAN,
                        isSafe ? TeleportHelperFilters.SURFACE_ONLY : NucleusTeleportHelperFilters.NO_CHECK
                );

        if (result.isSuccessful()) {
            // OK
            if (!playerToTeleport.equals(src)) {
                sendMessageTo(src,"command.top.success.other",
                        Nucleus.getNucleus().getNameUtil().getName(playerToTeleport));
            }

            sendMessageTo(src, "command.top.success.self");
            return CommandResult.success();
        }

        if (result == TeleportResults.FAIL_NO_LOCATION) {
            throw ReturnMessageException.fromKey(src, "command.top.notsafe");
        } else {
            throw ReturnMessageException.fromKey(src, "command.top.cancelled");
        }
    }
}
