/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.listeners;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.catalogkeys.NucleusTeleportHelperFilters;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportScanners;
import io.github.nucleuspowered.nucleus.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.interfaces.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.internal.traits.IDataManagerTrait;
import io.github.nucleuspowered.nucleus.internal.traits.MessageProviderTrait;
import io.github.nucleuspowered.nucleus.modules.core.CoreKeys;
import io.github.nucleuspowered.nucleus.modules.core.services.SafeTeleportService;
import io.github.nucleuspowered.nucleus.modules.spawn.SpawnKeys;
import io.github.nucleuspowered.nucleus.modules.spawn.config.GlobalSpawnConfig;
import io.github.nucleuspowered.nucleus.modules.spawn.config.SpawnConfig;
import io.github.nucleuspowered.nucleus.modules.spawn.config.SpawnConfigAdapter;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IGeneralDataObject;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.api.world.teleport.TeleportHelperFilters;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

public class SpawnListener implements Reloadable, ListenerBase, MessageProviderTrait, IDataManagerTrait {

    private SpawnConfig spawnConfig;

    private final String spawnExempt = PermissionRegistry.PERMISSIONS_PREFIX + "spawn.exempt.login";

    @Override
    public Map<String, PermissionInformation> getPermissions() {
        Map<String, PermissionInformation> mpi = Maps.newHashMap();
        mpi.put(this.spawnExempt, PermissionInformation.getWithTranslation("permission.spawn.exempt.login", SuggestedLevel.ADMIN));
        return mpi;
    }

    @Listener
    public void onJoin(ClientConnectionEvent.Login loginEvent) {
        UUID pl = loginEvent.getProfile().getUniqueId();
        boolean first = getOrCreateUserOnThread(pl).get(CoreKeys.FIRST_JOIN).isPresent();
        IGeneralDataObject generalDataObject = Nucleus.getNucleus().getStorageManager().getGeneralService().getOrNew().join();

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

                    Nucleus.getNucleus().getLogger().warn(
                            getMessageString("spawn.firstspawn.failed",
                                    loginEvent.getProfile().getName().orElse(getMessageString("standard.unknown"))));
                }
            }
        } catch (Exception e) {
            if (Nucleus.getNucleus().isDebugMode()) {
                e.printStackTrace();
            }
        }

        // Throw them to the default world spawn if the config suggests so.
        User user = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).getOrCreate(loginEvent.getProfile());
        if (this.spawnConfig.isSpawnOnLogin() && !hasPermission(user, this.spawnExempt)) {

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
            Optional<Location<World>> safe = getServiceUnchecked(SafeTeleportService.class)
                    .getSafeLocation(
                            lw,
                            TeleportScanners.ASCENDING_SCAN,
                            this.spawnConfig.isSafeTeleport() ? TeleportHelperFilters.DEFAULT : NucleusTeleportHelperFilters.NO_CHECK
                    );

            if (safe.isPresent()) {
                try {
                    Optional<Vector3d> ov = Nucleus.getNucleus()
                            .getStorageManager()
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
                Nucleus.getNucleus()
                        .getStorageManager()
                        .getWorldService()
                        .getOrNewOnThread(to.getExtent().getUniqueId())
                        .get(SpawnKeys.WORLD_SPAWN_ROTATION)
                        .ifPresent(y -> event.setToTransform(to.setRotation(y)));
            }
        }
    }

    @Listener(order = Order.EARLY)
    public void onRespawn(RespawnPlayerEvent event) {
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

        // Compare current transform to spawn - set rotation.
        Nucleus.getNucleus()
                .getStorageManager()
                .getWorldService()
                .getOrNewOnThread(world.getUniqueId())
                .get(SpawnKeys.WORLD_SPAWN_ROTATION)
                .ifPresent(y -> event.setToTransform(to.setRotation(y)));
    }

    @Override public void onReload() {
        this.spawnConfig = getServiceUnchecked(SpawnConfigAdapter.class).getNodeOrDefault();
    }

    private static Location<World> process(Location<World> v3d) {
        return new Location<>(v3d.getExtent(), process(v3d.getPosition()));
    }

    private static Vector3d process(Vector3d v3d) {
        return v3d.floor().add(0.5d, 0, 0.5d);
    }
}
