/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.listeners;

import com.flowpowered.math.vector.Vector3d;
import io.github.nucleuspowered.nucleus.api.EventContexts;
import io.github.nucleuspowered.nucleus.api.teleport.data.NucleusTeleportHelperFilters;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanners;
import io.github.nucleuspowered.nucleus.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.modules.core.CoreKeys;
import io.github.nucleuspowered.nucleus.modules.spawn.SpawnKeys;
import io.github.nucleuspowered.nucleus.modules.spawn.SpawnPermissions;
import io.github.nucleuspowered.nucleus.modules.spawn.config.GlobalSpawnConfig;
import io.github.nucleuspowered.nucleus.modules.spawn.config.SpawnConfig;
import io.github.nucleuspowered.nucleus.modules.spawn.events.SendToSpawnEvent;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IGeneralDataObject;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.services.interfaces.IStorageManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.api.world.teleport.TeleportHelperFilters;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;

public class SpawnListener implements IReloadableService.Reloadable, ListenerBase {

    private SpawnConfig spawnConfig;

    private final INucleusServiceCollection serviceCollection;

    @Inject
    public SpawnListener(INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
    }

    @Listener
    public void onJoin(ClientConnectionEvent.Login loginEvent) {
        UUID pl = loginEvent.getProfile().getUniqueId();
        IStorageManager storageManager = this.serviceCollection.storageManager();
        IMessageProviderService messageProviderService = this.serviceCollection.messageProvider();
        boolean first = storageManager.getOrCreateUserOnThread(pl).get(CoreKeys.FIRST_JOIN_PROCESSED).orElse(false);
        IGeneralDataObject generalDataObject = storageManager.getGeneralService().getOrNew().join();

        try {
            if (first) {
                // first spawn.
                Optional<Transform<World>> ofs = generalDataObject.get(SpawnKeys.FIRST_SPAWN_LOCATION)
                        .flatMap(LocationNode::getTransformIfExists);

                // Bit of an odd line, but what what is going on here is checking for first spawn, and if it exists, then
                // setting the location the player safely. If this cannot be done in either case, send them to world spawn.
                if (ofs.isPresent()) {
                    @Nullable Location<World> location;
                    if (this.spawnConfig.isSafeTeleport()) {
                        location = Sponge.getTeleportHelper().getSafeLocation(ofs.get().getLocation()).orElse(null);
                    } else {
                        location = ofs.get().getLocation();
                    }

                    if (location != null) {
                        loginEvent.setToTransform(new Transform<>(location.getExtent(), process(location.getPosition()), ofs.get().getRotation()));
                        return;
                    }

                    this.serviceCollection.logger()
                            .warn(
                                    messageProviderService.getMessageString(
                                            "spawn.firstspawn.failed",
                                            loginEvent.getProfile().getName().orElseGet(() ->
                                                    messageProviderService.getMessageString("standard.unknown")))
                            );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Throw them to the default world spawn if the config suggests so.
        User user = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).getOrCreate(loginEvent.getProfile());
        if (this.spawnConfig.isSpawnOnLogin() && !this.serviceCollection.permissionService().hasPermission(user, SpawnPermissions.SPAWN_EXEMPT_LOGIN)) {

            World world = loginEvent.getFromTransform().getExtent();
            final String worldName = world.getName();
            final String uuid = world.getUniqueId().toString();
            if (this.spawnConfig.getOnLoginExemptWorlds().stream().anyMatch(x -> x.equalsIgnoreCase(worldName) || x.equalsIgnoreCase(uuid))) {
                // we don't do this, exempt
                return;
            }

            GlobalSpawnConfig sc = this.spawnConfig.getGlobalSpawn();
            if (sc.isOnLogin() && sc.getWorld().isPresent()) {
                world = Sponge.getServer().getWorld(sc.getWorld().get().getUniqueId()).orElse(world);
            }

            Location<World> lw = world.getSpawnLocation().add(0.5, 0, 0.5);
            Optional<Location<World>> safe = this.serviceCollection.teleportService()
                    .getSafeLocation(
                            lw,
                            TeleportScanners.ASCENDING_SCAN.get(),
                            this.spawnConfig.isSafeTeleport() ? TeleportHelperFilters.DEFAULT : NucleusTeleportHelperFilters.NO_CHECK.get()
                    );

            if (safe.isPresent()) {
                try {
                    Optional<Vector3d> ov = storageManager
                            .getWorldService()
                            .getOrNewOnThread(world.getUniqueId())
                            .get(SpawnKeys.WORLD_SPAWN_ROTATION);
                    if (ov.isPresent()) {
                        loginEvent.setToTransform(new Transform<>(safe.get().getExtent(),
                                process(safe.get().getPosition()),
                                ov.get()));
                        return;
                    }
                } catch (Exception e) {
                    //
                }

                loginEvent.setToTransform(new Transform<>(process(safe.get())));
            }
        }
    }

    @Listener(order = Order.EARLY)
    public void onPlayerWorldTransfer(MoveEntityEvent.Teleport event) {
        if (event.getTargetEntity() instanceof Player && !event.getFromTransform().getExtent().equals(event.getToTransform().getExtent())) {
            // Are we heading TO a spawn?
            Transform<World> to = event.getToTransform();
            if (to.getLocation().getBlockPosition().equals(to.getExtent().getSpawnLocation().getBlockPosition())) {
                this.serviceCollection.storageManager()
                        .getWorldService()
                        .getOrNewOnThread(to.getExtent().getUniqueId())
                        .get(SpawnKeys.WORLD_SPAWN_ROTATION)
                        .ifPresent(y -> event.setToTransform(to.setRotation(y)));
            }
        }
    }

    @Listener(order = Order.EARLY)
    public void onRespawn(RespawnPlayerEvent event, @Getter("getTargetEntity") Player player) {
        if (event.isBedSpawn() && !this.spawnConfig.isRedirectBedSpawn()) {
            // Nope, we don't care.
            return;
        }

        GlobalSpawnConfig sc = this.spawnConfig.getGlobalSpawn();
        World world = event.getToTransform().getExtent();

        // Get the world.
        if (sc.isOnRespawn()) {
            Optional<WorldProperties> oworld = sc.getWorld();
            if (oworld.isPresent()) {
                world = Sponge.getServer().getWorld(oworld.get().getUniqueId()).orElse(world);
            }
        }

        Location<World> spawn = world.getSpawnLocation().add(0.5, 0, 0.5);
        Transform<World> to = new Transform<>(spawn);

        SendToSpawnEvent sEvent;
        try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.addContext(EventContexts.SPAWN_EVENT_TYPE, SendToSpawnEvent.Type.DEATH);
            frame.pushCause(player);
            sEvent = new SendToSpawnEvent(to, player, frame.getCurrentCause());
        }

        if (Sponge.getEventManager().post(sEvent)) {
            if (sEvent.getCancelReason().isPresent()) {
                this.serviceCollection.messageProvider().sendMessageTo(player, "command.spawnother.self.failed.reason", sEvent.getCancelReason().get());
                return;
            }

            this.serviceCollection.messageProvider().sendMessageTo(player, "command.spawnother.self.failed.noreason");
            return;
        }

        // Compare current transform to spawn - set rotation.
        this.serviceCollection.storageManager()
                .getWorldService()
                .getOrNewOnThread(world.getUniqueId())
                .get(SpawnKeys.WORLD_SPAWN_ROTATION)
                .ifPresent(y -> event.setToTransform(sEvent.isRedirected() ? sEvent.getTransformTo() : to.setRotation(y)));
    }

    @Override public void onReload(INucleusServiceCollection serviceCollection) {
        this.spawnConfig = serviceCollection.moduleDataProvider().getModuleConfig(SpawnConfig.class);
    }

    private static Location<World> process(Location<World> v3d) {
        return new Location<>(v3d.getExtent(), process(v3d.getPosition()));
    }

    private static Vector3d process(Vector3d v3d) {
        return v3d.floor().add(0.5d, 0, 0.5d);
    }
}
