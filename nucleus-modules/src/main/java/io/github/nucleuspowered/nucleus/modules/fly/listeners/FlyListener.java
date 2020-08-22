/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.fly.listeners;

import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanners;
import io.github.nucleuspowered.nucleus.modules.fly.FlyKeys;
import io.github.nucleuspowered.nucleus.modules.fly.FlyPermissions;
import io.github.nucleuspowered.nucleus.modules.fly.config.FlyConfig;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
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
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.World;

public class FlyListener implements IReloadableService.Reloadable, ListenerBase {

    private final INucleusServiceCollection serviceCollection;
    private FlyConfig flyConfig;

    public FlyListener(INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
        this.flyConfig = serviceCollection.moduleDataProvider().getDefaultModuleConfig(FlyConfig.class);
    }

    // Do it first, so other plugins can have a say.
    @Listener(order = Order.FIRST)
    public void onPlayerJoin(ClientConnectionEvent.Join event, @Getter("getTargetEntity") Player pl) {
        if (shouldIgnoreFromGameMode(pl)) {
            return;
        }

        if (this.flyConfig.isPermissionOnLogin() && !this.serviceCollection.permissionService().hasPermission(pl, FlyPermissions.BASE_FLY)) {
            safeTeleport(pl);
            return;
        }

        this.serviceCollection.storageManager().getUser(pl.getUniqueId()).thenAccept(x -> x.ifPresent(y -> {
            if (y.get(FlyKeys.FLY_TOGGLE).orElse(false)) {
                if (Sponge.getServer().isMainThread()) {
                    exec(pl);
                } else {
                    Task.builder().execute(() -> exec(pl)).submit(this.serviceCollection.pluginContainer());
                }
            } else {
                if (Sponge.getServer().isMainThread()) {
                    safeTeleport(pl);
                } else {
                    Task.builder().execute(() -> safeTeleport(pl)).submit(this.serviceCollection.pluginContainer());
                }
            }
        }));
    }

    private void exec(Player pl) {
        pl.offer(Keys.CAN_FLY, true);

        // If in the air, flying!
        if (pl.getLocation().add(0, -1, 0).getBlockType().getId().equals(BlockTypes.AIR.getId())) {
            pl.offer(Keys.IS_FLYING, true);
        }
    }

    @Listener
    public void onPlayerQuit(ClientConnectionEvent.Disconnect event, @Getter("getTargetEntity") Player pl) {
        if (!this.flyConfig.isSaveOnQuit()) {
            return;
        }

        if (shouldIgnoreFromGameMode(pl)) {
            return;
        }

        this.serviceCollection.storageManager().getOrCreateUser(pl.getUniqueId())
                .thenAccept(x -> x.set(FlyKeys.FLY_TOGGLE, pl.get(Keys.CAN_FLY).orElse(false)));

    }

    // Only fire if there is no cancellation at the end.
    @Listener(order = Order.LAST)
    public void onPlayerTransferWorld(MoveEntityEvent.Teleport event,
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

        // If we have a subject, and this happens...
        boolean isFlying = target.get(Keys.IS_FLYING).orElse(false);

        // If we're moving world...
        if (!twfrom.getExtent().getUniqueId().equals(twto.getExtent().getUniqueId())) {
            // Next tick, they can fly... if they have permission to do so.
            Sponge.getScheduler().createTaskBuilder().execute(() -> {
                if (this.serviceCollection.permissionService().hasPermission(pl, FlyPermissions.BASE_FLY)) {
                    target.offer(Keys.CAN_FLY, true);
                    if (isFlying) {
                        target.offer(Keys.IS_FLYING, true);
                    }
                } else {
                    this.serviceCollection.storageManager().getOrCreateUser(pl.getUniqueId()).thenAccept(x -> x.set(FlyKeys.FLY_TOGGLE, false));
                    target.offer(Keys.CAN_FLY, false);
                    target.offer(Keys.IS_FLYING, false);
                }
            }).submit(this.serviceCollection.pluginContainer());
        }
    }

    static boolean shouldIgnoreFromGameMode(Player player) {
        GameMode gm = player.get(Keys.GAME_MODE).orElse(GameModes.NOT_SET);
        return (gm.equals(GameModes.CREATIVE) || gm.equals(GameModes.SPECTATOR));
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        this.flyConfig = this.serviceCollection.moduleDataProvider().getModuleConfig(FlyConfig.class);
    }

    private void safeTeleport(Player pl) {
        if (!pl.isOnGround() && this.flyConfig.isFindSafeOnLogin()) {
            // Try to bring the subject down.
            this.serviceCollection
                    .teleportService()
                    .teleportPlayerSmart(
                            pl,
                            pl.getTransform(),
                            false,
                            true,
                            TeleportScanners.DESCENDING_SCAN.get()
                    );
        }
    }
}
