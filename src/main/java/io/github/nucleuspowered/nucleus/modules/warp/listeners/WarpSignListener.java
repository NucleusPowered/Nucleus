/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.listeners;

import com.flowpowered.math.vector.Vector3d;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.data.WarpLocation;
import io.github.nucleuspowered.nucleus.api.spongedata.warp.WarpSignData;
import io.github.nucleuspowered.nucleus.internal.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.services.WarmupManager;
import io.github.nucleuspowered.nucleus.modules.warp.config.WarpConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.warp.handlers.WarpHandler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class WarpSignListener extends ListenerBase {

    @Inject private WarpConfigAdapter wca;
    @Inject private WarmupManager warmupManager;
    @Inject private WarpHandler handler;

    @Listener
    public void onInteract(InteractEvent event, @Root Player player) {
        if (!wca.getNodeOrDefault().areWarpSignsEnabled()) {
            return;
        }

        if (!event.getInteractionPoint().isPresent()) {
            return;
        }

        Location<World> lw = new Location<World>(player.getWorld(), event.getInteractionPoint().get());
        if (lw.getTileEntity().isPresent() && lw.getTileEntity().get() instanceof Sign) {
            Sign sign = (Sign)lw.getTileEntity().get();
            Optional<WarpSignData> ow = sign.get(WarpSignData.class);
            if (ow.isPresent()) {
                WarpSignData w = ow.get();
                String warpName = w.warpName().or("").get();
                if (!handler.warpExists(warpName)) {
                    player.sendMessage(Util.getTextMessageWithFormat("warpsign.warp.error"));
                    return;
                }

                Optional<String> permission = w.permission().get();
                if (permission.isPresent() && !player.hasPermission(permission.get())) {
                    player.sendMessage(Util.getTextMessageWithFormat("warpsign.warp.nopermission"));
                    return;
                }

                WarpLocation wd = handler.getWarp(warpName).get();
                int warmup = w.warmupTime().get();
                if (warmup > 0) {
                    player.sendMessage(Util.getTextMessageWithFormat("warmup.start", Util.getTimeStringFromSeconds(warmup)));
                    warmupManager.addWarmup(player.getUniqueId(), Sponge.getScheduler().createSyncExecutor(plugin).schedule(() -> {
                        player.sendMessage(Util.getTextMessageWithFormat("warmup.end"));
                        warmupManager.removeWarmup(player.getUniqueId());
                        warpPlayer(player, warpName, wd.getLocation(), wd.getRotation());
                    }, warmup, TimeUnit.SECONDS).getTask());
                } else {
                     warpPlayer(player, warpName, wd.getLocation(), wd.getRotation());
                }
            }
        }
    }

    private void warpPlayer(Player player, String warp, Location<World> worldLocation, Vector3d rotation) {
        player.sendMessage(Util.getTextMessageWithFormat("command.warps.start", warp));

        // Warp them.
         if (!player.setLocationAndRotationSafely(worldLocation, rotation)) {
            player.sendMessage(Util.getTextMessageWithFormat("command.warps.nosafe"));
        }
    }
}
