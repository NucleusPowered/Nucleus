/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport.commands;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanners;
import io.github.nucleuspowered.nucleus.modules.teleport.TeleportPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.INucleusTeleportService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.CommandFlags;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

@NonnullByDefault
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
public class TeleportPositionCommand implements ICommandExecutor<CommandSource> {

    private final String location = "world";
//    private final String p = "pitch";
//    private final String yaw = "yaw";

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                GenericArguments.flags()
                    .setUnknownShortFlagBehavior(CommandFlags.UnknownFlagBehavior.IGNORE)
                    .flag("f", "-force")
                    .flag("c", "-chunk")
//                    .valueFlag(GenericArguments.doubleNum(Text.of(this.p)), "p", "-pitch")
//                    .valueFlag(GenericArguments.doubleNum(Text.of(this.yaw)), "y", "-yaw")
                    .permissionFlag(TeleportPermissions.TPPOS_BORDER,"b", "-border")
                    .buildWith(
                        GenericArguments.seq(
                            // Actual arguments
                                serviceCollection.commandElementSupplier()
                                    .createOtherUserPermissionElement(true, TeleportPermissions.OTHERS_TPPOS),
                            GenericArguments.onlyOne(GenericArguments.optionalWeak(GenericArguments.world(Text.of(this.location)))),
                            NucleusParameters.POSITION
                        )
                )
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Player pl = context.getPlayerFromArgs();
        WorldProperties wp = context.getOne(this.location, WorldProperties.class).orElseGet(() -> pl.getWorld().getProperties());
        World world = Sponge.getServer().loadWorld(wp.getUniqueId()).get();
        Vector3d location = context.requireOne(NucleusParameters.Keys.XYZ, Vector3d.class);

        double xx = location.getX();
        double  zz = location.getZ();
        double  yy = location.getY();
        if (yy < 0) {
            return context.errorResult("command.tppos.ysmall");
        }

        // Chunks are 16 in size, chunk 0 is from 0 - 15, -1 from -1 to -16.
        if (context.hasAny("c")) {
            xx = xx * 16 + 8;
            yy = yy * 16 + 8;
            zz = zz * 16 + 8;
            context.sendMessage("command.tppos.fromchunk", xx, yy, zz);
        }

        Vector3i max = world.getBlockMax();
        Vector3i min = world.getBlockMin();
        if (!(isBetween(xx, max.getX(), min.getX()) && isBetween(yy, max.getY(), min.getY()) && isBetween(zz, max.getZ(), min.getZ()))) {
            return context.errorResult("command.tppos.invalid");
        }

        // Create the location
        Location<World> loc = new Location<>(world, xx, yy, zz);
        INucleusTeleportService teleportHandler = context.getServiceCollection().teleportService();

        boolean safe = context.getOne("f", Boolean.class).orElse(false);
        boolean border = context.hasAny("b");

        try (INucleusTeleportService.BorderDisableSession ac =
                teleportHandler.temporarilyDisableBorder(!safe && border, loc.getExtent())) {
            TeleportResult result = teleportHandler.teleportPlayerSmart(
                    pl,
                    loc,
                    false,
                    safe,
                    TeleportScanners.NO_SCAN.get()
            );

            if (result.isSuccessful()) {
                context.sendMessageTo(pl, "command.tppos.success.self");
                if (!context.is(pl)) {
                    context.sendMessage("command.tppos.success.other", pl.getName());
                }

                return context.successResult();
            } else if (result == TeleportResult.FAIL_NO_LOCATION) {
                return context.errorResult("command.tppos.nosafe");
            }

            return context.errorResult("command.tppos.cancelledevent");
        }
    }

    private boolean isBetween(double toCheck, double max, double min) {
        return toCheck >= min && toCheck <= max;
    }
}
