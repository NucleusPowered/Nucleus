/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport.commands;

import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanners;
import io.github.nucleuspowered.nucleus.modules.teleport.TeleportPermissions;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.INucleusLocationService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;

@EssentialsEquivalent("tppos")
@Command(
        aliases = "tppos",
        basePermission = TeleportPermissions.BASE_TPPOS,
        commandDescriptionKey = "tppos",
        associatedPermissions = {
                TeleportPermissions.TPPOS_BORDER,
                TeleportPermissions.OTHERS_TPPOS
        }
)
public class TeleportPositionCommand implements ICommandExecutor {

    @Override
    public Flag[] flags(final INucleusServiceCollection serviceCollection) {
        final IPermissionService permissionService = serviceCollection.permissionService();
        return new Flag[] {
                Flag.of("f", "force"),
                Flag.of("c", "chunk"),
                Flag.builder().setRequirement(cause -> permissionService.hasPermission(cause, TeleportPermissions.TPPOS_BORDER))
                        .aliases("b", "border").build()
        };
    }

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                serviceCollection.commandElementSupplier().createOnlyOtherUserPermissionElement(TeleportPermissions.OTHERS_TPPOS),
                NucleusParameters.LOCATION
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final User pl = context.getUserFromArgs();
        final ServerLocation location = context.requireOne(NucleusParameters.LOCATION);
        final ServerWorld world = location.world();

        double xx = location.x();
        double zz = location.z();
        double yy = location.y();
        if (yy < 0) {
            return context.errorResult("command.tppos.ysmall");
        }

        // Chunks are 16 in size, chunk 0 is from 0 - 15, -1 from -1 to -16.
        if (context.hasFlag("c")) {
            xx = xx * 16 + 8;
            yy = yy * 16 + 8;
            zz = zz * 16 + 8;
            context.sendMessage("command.tppos.fromchunk", xx, yy, zz);
        }

        final Vector3i max = world.max();
        final Vector3i min = world.min();
        if (!(this.isBetween(xx, max.x(), min.x()) && this.isBetween(yy, max.y(), min.y()) && this.isBetween(zz, max.z(), min.z()))) {
            return context.errorResult("command.tppos.invalid");
        }

        // Create the location
        final ServerLocation loc = ServerLocation.of(world, xx, yy, zz);
        final INucleusLocationService teleportHandler = context.getServiceCollection().teleportService();

        final boolean safe = context.hasFlag("f");
        final boolean border = context.hasFlag("b");

        if (!pl.isOnline()) {
            pl.setLocation(loc.worldKey(), loc.position());
            context.sendMessage("command.tppos.success.other", pl.name());
            return context.successResult();
        }

        final ServerPlayer player = pl.player().get();
        try (final INucleusLocationService.BorderDisableSession ac =
                teleportHandler.temporarilyDisableBorder(!safe && border, loc.world())) {
            final TeleportResult result = teleportHandler.teleportPlayerSmart(
                    player,
                    loc,
                    false,
                    safe,
                    TeleportScanners.NO_SCAN.get()
            );

            if (result.isSuccessful()) {
                context.sendMessageTo(player, "command.tppos.success.self");
                if (!context.is(pl)) {
                    context.sendMessage("command.tppos.success.other", pl.name());
                }

                return context.successResult();
            } else if (result == TeleportResult.FAIL_NO_LOCATION) {
                return context.errorResult("command.tppos.nosafe");
            }

            return context.errorResult("command.tppos.cancelledevent");
        }
    }

    private boolean isBetween(final double toCheck, final double max, final double min) {
        return toCheck >= min && toCheck <= max;
    }
}
