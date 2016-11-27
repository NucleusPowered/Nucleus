/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jump.commands;

import io.github.nucleuspowered.nucleus.argumentparsers.SelectorWrapperArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.Map;

@Permissions(supportsSelectors = true)
@RegisterCommand({"top", "tosurface", "totop"})
public class TopCommand extends AbstractCommand<CommandSource> {

    private final String playerKey = "player";

    @Override protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        return new HashMap<String, PermissionInformation>() {{
            put("others", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.top.others"), SuggestedLevel.ADMIN));
        }};
    }

    @Override public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.flags().flag("f").buildWith(
                GenericArguments.optional(
                GenericArguments.requiringPermission(
                    GenericArguments.onlyOne(
                        new SelectorWrapperArgument(GenericArguments.player(Text.of(playerKey)), permissions, SelectorWrapperArgument.SINGLE_PLAYER_SELECTORS)),
                    permissions.getPermissionWithSuffix("others"))))
        };
    }

    @Override public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        Player playerToTeleport = this.getUserFromArgs(Player.class, src, playerKey, args);

        // Get the topmost block for the player.
        Location<World> location = playerToTeleport.getLocation();
        double x = location.getX();
        double z = location.getZ();
        Location<World> start = new Location<>(location.getExtent(), x, location.getExtent().getBlockMax().getY(), z);
        BlockRayHit<World> end = BlockRay.from(start).stopFilter(BlockRay.onlyAirFilter())
            .to(playerToTeleport.getLocation().getPosition().sub(0, 1, 0)).end()
            .orElseThrow(() -> new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.top.nothingfound")));

        if (playerToTeleport.getLocation().getBlockPosition().equals(end.getBlockPosition())) {
            if (!playerToTeleport.equals(src)) {
                throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.top.attop.other", plugin.getNameUtil().getSerialisedName(playerToTeleport)));
            } else {
                throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.top.attop.self"));
            }
        }

        if (plugin.getTeleportHandler().teleportPlayer(playerToTeleport, end.getLocation(), !args.hasAny("f"))) {
            // OK
            if (!playerToTeleport.equals(src)) {
                src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.top.success.other", plugin.getNameUtil().getSerialisedName(playerToTeleport)));
            }

            playerToTeleport.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.top.success.self"));
            return CommandResult.success();
        }

        throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.top.notsafe"));
    }
}
