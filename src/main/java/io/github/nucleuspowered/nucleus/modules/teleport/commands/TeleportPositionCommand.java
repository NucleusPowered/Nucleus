/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport.commands;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportResults;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportScanners;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.core.services.SafeTeleportService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.CommandFlags;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.HashMap;
import java.util.Map;

@Permissions(prefix = "teleport", supportsOthers = true)
@NoModifiers
@NonnullByDefault
@RegisterCommand({"tppos"})
@EssentialsEquivalent("tppos")
public class TeleportPositionCommand extends AbstractCommand<CommandSource> {

    private final String location = "world";
//    private final String p = "pitch";
//    private final String yaw = "yaw";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.flags()
                    .setUnknownShortFlagBehavior(CommandFlags.UnknownFlagBehavior.IGNORE)
                    .flag("f", "-force")
                    .flag("c", "-chunk")
//                    .valueFlag(GenericArguments.doubleNum(Text.of(this.p)), "p", "-pitch")
//                    .valueFlag(GenericArguments.doubleNum(Text.of(this.yaw)), "y", "-yaw")
                    .permissionFlag(this.permissions.getPermissionWithSuffix("exempt.bordercheck"),"b", "-border")
                    .buildWith(
                        GenericArguments.seq(
                            // Actual arguments
                            GenericArguments.optionalWeak(
                                    requirePermissionArg(NucleusParameters.ONE_PLAYER, this.permissions.getOthers())),
                            GenericArguments.onlyOne(GenericArguments.optionalWeak(GenericArguments.world(Text.of(this.location)))),
                            NucleusParameters.POSITION
                        )
                )
        };
    }

    @Override
    protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        return new HashMap<String, PermissionInformation>() {{
            put("exempt.bordercheck", PermissionInformation.getWithTranslation("permission.tppos.border", SuggestedLevel.ADMIN));
        }};
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) throws Exception {
        Player pl = this.getUserFromArgs(Player.class, src, NucleusParameters.Keys.PLAYER, args);
        WorldProperties wp = args.<WorldProperties>getOne(this.location).orElse(pl.getWorld().getProperties());
        World world = Sponge.getServer().loadWorld(wp.getUniqueId()).get();
        Vector3d location = args.<Vector3d>getOne(NucleusParameters.Keys.XYZ).get();

        double xx = location.getX();
        double  zz = location.getZ();
        double  yy = location.getY();
        if (yy < 0) {
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.tppos.ysmall"));
            return CommandResult.empty();
        }

        // Chunks are 16 in size, chunk 0 is from 0 - 15, -1 from -1 to -16.
        if (args.hasAny("c")) {
            xx = xx * 16 + 8;
            yy = yy * 16 + 8;
            zz = zz * 16 + 8;
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.tppos.fromchunk",
                    String.valueOf(xx), String.valueOf(yy), String.valueOf(zz)));
        }

        Vector3i max = world.getBlockMax();
        Vector3i min = world.getBlockMin();
        if (!(isBetween(xx, max.getX(), min.getX()) && isBetween(yy, max.getY(), min.getY()) && isBetween(zz, max.getZ(), min.getZ()))) {
            throw ReturnMessageException.fromKey("command.tppos.invalid");
        }

        // Create the location
        Location<World> loc = new Location<>(world, xx, yy, zz);
        SafeTeleportService teleportHandler = getServiceUnchecked(SafeTeleportService.class);

        boolean safe = args.<Boolean>getOne("f").orElse(false);
        boolean border = args.hasAny("b");

        try (AutoCloseable ac = teleportHandler.temporarilyDisableBorder(!safe && border, loc.getExtent())) {
            TeleportResult result = teleportHandler.teleportPlayerSmart(
                    pl,
                    loc,
                    false,
                    safe,
                    TeleportScanners.NO_SCAN
            );

            if (result.isSuccessful()) {
                pl.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.tppos.success.self"));
                if (!src.equals(pl)) {
                    src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.tppos.success.other", pl.getName()));
                }

                return CommandResult.success();
            } else if (result == TeleportResults.FAIL_NO_LOCATION) {
                throw ReturnMessageException.fromKey("command.tppos.nosafe");
            }

            throw ReturnMessageException.fromKey("command.tppos.cancelledevent");
        }
    }

    private boolean isBetween(double toCheck, double max, double min) {
        return toCheck >= min && toCheck <= max;
    }
}
