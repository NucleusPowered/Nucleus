/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jump.commands;

import io.github.nucleuspowered.nucleus.api.teleport.data.NucleusTeleportHelperFilters;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanners;
import io.github.nucleuspowered.nucleus.modules.jump.JumpPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.teleport.TeleportHelperFilters;

@NonnullByDefault
@Command(
        aliases = {"top", "tosurface", "totop"},
        basePermission = JumpPermissions.BASE_TOP,
        commandDescriptionKey = "top",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = JumpPermissions.EXEMPT_COOLDOWN_TOP),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = JumpPermissions.EXEMPT_WARMUP_TOP),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = JumpPermissions.EXEMPT_COST_TOP)
        },
        associatedPermissions = JumpPermissions.OTHERS_TOP
)
public class TopCommand implements ICommandExecutor<CommandSource> {

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
            GenericArguments.flags().flag("f").buildWith(
                    serviceCollection.commandElementSupplier()
                        .createOnlyOtherUserPermissionElement(true, JumpPermissions.OTHERS_TOP)
            )
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Player playerToTeleport = context.getPlayerFromArgs();

        // Get the topmost block for the subject.
        Location<World> location = playerToTeleport.getLocation();
        double x = location.getX();
        double z = location.getZ();
        Location<World> start = new Location<>(location.getExtent(), x, location.getExtent().getBlockMax().getY(), z);
        BlockRayHit<World> end = BlockRay.from(start).stopFilter(BlockRay.onlyAirFilter())
            .to(playerToTeleport.getLocation().getPosition().sub(0, 1, 0)).end()
            .orElseThrow(() -> context.createException("command.top.nothingfound"));

        if (playerToTeleport.getLocation().getBlockPosition().equals(end.getBlockPosition())) {
            if (!context.is(playerToTeleport)) {
                return context.errorResult(
                        "command.top.attop.other",
                        context.getDisplayName(playerToTeleport.getUniqueId()));
            } else {
                return context.errorResult("command.top.attop.self");
            }
        }

        boolean isSafe = !context.hasAny("f");
        TeleportResult result = context.getServiceCollection()
                .teleportService()
                .teleportPlayer(
                        playerToTeleport,
                        end.getLocation(),
                        playerToTeleport.getRotation(),
                        false,
                        TeleportScanners.NO_SCAN.get(),
                        isSafe ? TeleportHelperFilters.SURFACE_ONLY : NucleusTeleportHelperFilters.NO_CHECK.get()
                );

        if (result.isSuccessful()) {
            // OK
            if (!context.is(playerToTeleport)) {
                context.sendMessage("command.top.success.other", context.getDisplayName(playerToTeleport.getUniqueId()));
            }

            context.sendMessageTo(playerToTeleport, "command.top.success.self");
            return context.successResult();
        }

        if (result == TeleportResult.FAIL_NO_LOCATION) {
            return context.errorResult("command.top.notsafe");
        } else {
            return context.errorResult("command.top.cancelled");
        }
    }
}
