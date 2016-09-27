/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.commands.signs;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.spongedata.warp.WarpSignData;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.warp.config.WarpConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.warp.handlers.WarpHandler;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

/**
 * When looking at a warp sign, this tells the user where the sign leads.
 */
@Permissions(suggestedLevel = SuggestedLevel.USER)
@RegisterCommand("warpsign")
@NoCost
@NoCooldown
@NoWarmup
public class WarpSignCommand extends AbstractCommand<Player> {

    @Inject private WarpConfigAdapter wca;
    @Inject private WarpHandler warpHandler;
    @Inject private PermissionRegistry pr;

    @Override
    public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        if (!wca.getNodeOrDefault().areWarpSignsEnabled()) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.warpsign.notenabled"));
            return CommandResult.empty();
        }

        // Is there a sign?
        Optional<Sign> os = getSignFromBlockRay(src);
        if (os.isPresent()) {
            // Is there any data attached to the sign?
            Sign sign = os.get();
            Optional<WarpSignData> wsd = sign.get(WarpSignData.class);
            if (wsd.isPresent()) {
                displayWarpData(src, wsd.get());
            } else {
                src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.warpsign.nodata"));
            }
        } else {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.warpsign.nosign"));
        }

        // Just informational, it's always a success.
        return CommandResult.success();
    }

    private void displayWarpData(Player player, WarpSignData data) {
        String warp = data.warpName().get().orElse("");
        if (warpHandler.warpExists(warp)) {
            player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.warpsign.warpinfo.standard", warp, data.warmupTime().get().toString()));

            Optional<String> permission = data.permission().get();
            if (permission.isPresent() && !permission.get().isEmpty()) {
                if (!player.hasPermission(permission.get())) {
                    player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.warpsign.warpinfo.noperm"));
                }

                if (pr.getService(SetWarpSignCommand.class).get().testBase(player)) {
                    player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.warpsign.warpinfo.admin.perm", permission.get()));
                }
            }
        } else {
            player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.warpsign.invalidwarp", warp));
        }
    }

    static Optional<Sign> getSignFromBlockRay(Entity from) {
        BlockRay<World> bw = BlockRay.from(from).blockLimit(15).filter(BlockRay.continueAfterFilter(BlockRay.onlyAirFilter(), 1)).build();
        Optional<BlockRayHit<World>> obh = bw.end();
        if (obh.isPresent()) {
            Location<World> lw = obh.get().getLocation();
            if (lw.getTileEntity().isPresent() && lw.getTileEntity().get() instanceof Sign) {
                return Optional.of((Sign)lw.getTileEntity().get());
            }
        }

        return Optional.empty();
    }
}
