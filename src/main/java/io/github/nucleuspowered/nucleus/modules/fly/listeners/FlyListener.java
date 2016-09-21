/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.fly.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.dataservices.UserService;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.internal.listeners.ListenerBase;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.fly.config.FlyConfigAdapter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.DismountEntityEvent;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.world.World;

public class FlyListener extends ListenerBase {

    @Inject private UserDataManager ucl;
    @Inject private CoreConfigAdapter cca;
    @Inject private FlyConfigAdapter fca;

    // Do it first, so other plugins can have a say.
    @Listener(order = Order.FIRST)
    public void onPlayerJoin(ClientConnectionEvent.Join event) {
        Player pl = event.getTargetEntity();
        if (shouldIgnoreFromGameMode(pl)) {
            return;
        }

        try {
            ucl.get(pl).ifPresent(uc -> {

                // Let's just reset these...
                if (uc.isFlyingSafe()) {
                    pl.offer(Keys.CAN_FLY, true);

                    // If in the air, flying!
                    if (pl.getLocation().add(0, -1, 0).getBlockType().getId().equals(BlockTypes.AIR.getId())) {
                        pl.offer(Keys.IS_FLYING, true);
                    }
                }
            });
        } catch (Exception e) {
            if (cca.getNodeOrDefault().isDebugmode()) {
                e.printStackTrace();
            }
        }
    }

    @Listener
    public void onPlayerQuit(ClientConnectionEvent.Disconnect event) {
        if (!fca.getNodeOrDefault().isSaveOnQuit()) {
            return;
        }

        Player pl = event.getTargetEntity();
        if (shouldIgnoreFromGameMode(pl)) {
            return;
        }

        try {
            ucl.getUser(pl).get().setFlying(pl.get(Keys.CAN_FLY).orElse(false));
        } catch (Exception e) {
            if (cca.getNodeOrDefault().isDebugmode()) {
                e.printStackTrace();
            }
        }
    }

    // Only fire if there is no cancellation at the end.
    @Listener(order = Order.LAST)
    public void onPlayerTransferWorld(DisplaceEntityEvent.Teleport event,
                                      @Getter("getTargetEntity") Entity target,
                                      @Getter("getFromTransform") Transform<World> twfrom,
                                      @Getter("getToTransform") Transform<World> twto) {

        if (!(target instanceof Player)) {
            return;
        }

        Player pl = (Player)target;
        if (shouldIgnoreFromGameMode(pl)) {
            return;
        }

        UserService uc;
        try {
            uc = ucl.get(pl).get();
            if (!uc.isFlying()) {
                return;
            }
        } catch (Exception e) {
            if (cca.getNodeOrDefault().isDebugmode()) {
                e.printStackTrace();
            }

            return;
        }

        // If we have a player, and this happens...
        boolean isFlying = target.get(Keys.IS_FLYING).orElse(false);

        // If we're moving world...
        if (!twfrom.getExtent().getUniqueId().equals(twto.getExtent().getUniqueId())) {
            // Next tick, they can fly...
            Sponge.getScheduler().createTaskBuilder().execute(() -> {
                target.offer(Keys.CAN_FLY, true);
                if (isFlying) {
                    target.offer(Keys.IS_FLYING, true);
                }
            }).submit(plugin);
        }
    }

    @Listener
    public void onPlayerDismount(DismountEntityEvent event, @Root Player player) {
        // If I'm right, this will work around Pixelmon when dismounting pokemon.
        if (shouldIgnoreFromGameMode(player)) {
            return;
        }

        try {
            ucl.get(player).ifPresent(x -> player.offer(Keys.CAN_FLY, x.isFlyingSafe()));
        } catch (Exception e) {
            if (cca.getNodeOrDefault().isDebugmode()) {
                e.printStackTrace();
            }
        }
    }

    private boolean shouldIgnoreFromGameMode(Player player) {
        GameMode gm = player.get(Keys.GAME_MODE).orElse(GameModes.NOT_SET);
        return (gm.equals(GameModes.CREATIVE) || gm.equals(GameModes.SPECTATOR));
    }
}
