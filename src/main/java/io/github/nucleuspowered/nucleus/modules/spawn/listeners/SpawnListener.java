/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.listeners;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.dataservices.GeneralService;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.dataservices.loaders.WorldDataManager;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.listeners.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.spawn.config.GlobalSpawnConfig;
import io.github.nucleuspowered.nucleus.modules.spawn.config.SpawnConfigAdapter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Map;
import java.util.Optional;

public class SpawnListener extends ListenerBase {

    @Inject private GeneralService store;
    @Inject private UserDataManager loader;
    @Inject private WorldDataManager wcl;
    @Inject private CoreConfigAdapter cca;
    @Inject private SpawnConfigAdapter sca;

    private final String spawnExempt = PermissionRegistry.PERMISSIONS_PREFIX + "spawn.exempt.login";

    @Override
    public Map<String, PermissionInformation> getPermissions() {
        Map<String, PermissionInformation> mpi = Maps.newHashMap();
        mpi.put(spawnExempt, new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.spawn.exempt.login"), SuggestedLevel.ADMIN));
        return mpi;
    }

    @Listener
    public void onJoin(ClientConnectionEvent.Join joinEvent) {
        Player pl = joinEvent.getTargetEntity();

        try {
            if (loader.getUser(pl).get().isFirstPlay()) {
                // first spawn.
                Optional<Transform<World>> ofs = store.getFirstSpawn();

                // Bit of an odd line, but what what is going on here is checking for first spawn, and if it exists, then
                // setting the location the player safely. If this cannot be done in either case, send them to world spawn.
                if (!ofs.isPresent() || !pl.setLocationAndRotationSafely(ofs.get().getLocation(), ofs.get().getRotation())) {
                    WorldProperties w = Sponge.getServer().getDefaultWorld().get();
                    pl.setLocation(new Location<>(Sponge.getServer().getWorld(w.getUniqueId()).get(), w.getSpawnPosition().toDouble()));

                    // We don't want to boot them elsewhere.
                    return;
                }
            }
        } catch (Exception e) {
            if (cca.getNodeOrDefault().isDebugmode()) {
                e.printStackTrace();
            }
        }

        // Throw them to the default world spawn if the config suggests so.
        if (sca.getNodeOrDefault().isSpawnOnLogin() && !pl.hasPermission(spawnExempt)) {

            GlobalSpawnConfig sc = sca.getNodeOrDefault().getGlobalSpawn();
            World world = joinEvent.getTargetEntity().getWorld();
            if (sc.isOnLogin()) {
                world = sc.getWorld().orElse(world);
            }

            Location<World> lw = world.getSpawnLocation();
            try {
                Optional<Vector3d> ov = wcl.getWorld(world.getUniqueId()).get().getSpawnRotation();
                if (ov.isPresent()) {
                    pl.setLocationAndRotation(lw, ov.get());
                }
            } catch (Exception e) {
                //
            }

            pl.setLocation(lw);
        }
    }

    @Listener(order = Order.FIRST)
    public void onRespawn(RespawnPlayerEvent event) {
        if (event.isBedSpawn()) {
            // Nope, we don't care.
            return;
        }

        GlobalSpawnConfig sc = sca.getNodeOrDefault().getGlobalSpawn();
        World world = event.getToTransform().getExtent();

        // Get the world.
        if (sc.isOnRespawn()) {
            Optional<World> oworld = sc.getWorld();
            if (oworld.isPresent()) {
                world = oworld.get();
            }
        }

        try {
            Location<World> spawn = world.getSpawnLocation();
            Transform<World> to = new Transform<>(spawn);
            Optional<Vector3d> ov = wcl.getWorld(world).get().getSpawnRotation();
            if (ov.isPresent()) {
                // Compare current transform to spawn.
                to.setRotation(ov.get());
            }

            event.setToTransform(to);
        } catch (Exception e) {
            if (cca.getNodeOrDefault().isDebugmode()) {
                e.printStackTrace();
            }
        }
    }
}
