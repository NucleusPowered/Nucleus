/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.api.teleport.data.NucleusTeleportHelperFilters;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanners;
import io.github.nucleuspowered.nucleus.core.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.core.core.CoreKeys;
import io.github.nucleuspowered.nucleus.modules.spawn.SpawnKeys;
import io.github.nucleuspowered.nucleus.modules.spawn.SpawnPermissions;
import io.github.nucleuspowered.nucleus.modules.spawn.config.GlobalSpawnConfig;
import io.github.nucleuspowered.nucleus.modules.spawn.config.SpawnConfig;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.modular.IGeneralDataObject;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IStorageManager;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.living.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.api.world.teleport.TeleportHelperFilters;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.util.Optional;
import java.util.UUID;

public class SpawnListener implements IReloadableService.Reloadable, ListenerBase {

    private SpawnConfig spawnConfig;
    private boolean checkSponge;

    private final INucleusServiceCollection serviceCollection;

    @Inject
    public SpawnListener(final INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
    }

    @Listener
    public void onJoin(final ServerSideConnectionEvent.Login loginEvent) {
        final UUID pl = loginEvent.profile().uniqueId();
        final IStorageManager storageManager = this.serviceCollection.storageManager();
        final IMessageProviderService messageProviderService = this.serviceCollection.messageProvider();
        final boolean first;
        if (!storageManager.getOrCreateUserOnThread(pl).get(CoreKeys.FIRST_JOIN_PROCESSED).orElse(false)) {
            first = !this.checkSponge || !Util.hasPlayedBeforeSponge(loginEvent.user());
        } else {
            first = false;
        }
        final IGeneralDataObject generalDataObject = storageManager.getGeneralService().getOrNew().join();

        try {
            if (first) {
                // first spawn.
                final Optional<LocationNode> locationNode = generalDataObject.get(SpawnKeys.FIRST_SPAWN_LOCATION);
                final Optional<ServerLocation> ofs = locationNode.flatMap(LocationNode::getLocationIfExists);

                // Bit of an odd line, but what what is going on here is checking for first spawn, and if it exists, then
                // setting the location the player safely. If this cannot be done in either case, send them to world spawn.
                if (ofs.isPresent()) {
                    @Nullable final ServerLocation location;
                    if (this.spawnConfig.isSafeTeleport()) {
                        location = Sponge.server().teleportHelper().findSafeLocation(ofs.get()).orElse(null);
                    } else {
                        location = ofs.get();
                    }

                    if (location != null) {
                        loginEvent.setToLocation(location);
                        loginEvent.setToRotation(locationNode.get().getRotation());
                        return;
                    }

                    this.serviceCollection.logger()
                            .warn(
                                    messageProviderService.getMessageString(
                                            "spawn.firstspawn.failed",
                                            loginEvent.profile().name().orElseGet(() ->
                                                    messageProviderService.getMessageString("standard.unknown")))
                            );
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

        // Throw them to the default world spawn if the config suggests so.
        final User user = Sponge.server().userManager().loadOrCreate(loginEvent.profile().uuid()).join();
        if (this.spawnConfig.isSpawnOnLogin() && !this.serviceCollection.permissionService().hasPermission(user, SpawnPermissions.SPAWN_EXEMPT_LOGIN)) {

            ServerWorld world = loginEvent.fromLocation().world();
            final ResourceKey worldName = world.key();
            if (this.spawnConfig.getOnLoginExemptWorlds().stream().anyMatch(x -> x.equalsIgnoreCase(worldName.asString()))) {
                // we don't do this, exempt
                return;
            }

            final GlobalSpawnConfig sc = this.spawnConfig.getGlobalSpawn();
            if (sc.isOnLogin()) {
                world = sc.getWorld().orElse(world);
            }

            final ServerLocation lw = ServerLocation.of(world.key(), world.properties().spawnPosition().add(0.5, 0, 0.5));
            final Optional<ServerLocation> safe = this.serviceCollection.teleportService()
                    .getSafeLocation(
                            lw,
                            TeleportScanners.ASCENDING_SCAN.get(),
                            this.spawnConfig.isSafeTeleport() ? TeleportHelperFilters.DEFAULT.get() : NucleusTeleportHelperFilters.NO_CHECK.get()
                    );

            if (safe.isPresent()) {
                loginEvent.setToLocation(SpawnListener.process(safe.get()));
                try {
                    storageManager
                            .getWorldService()
                            .getOrNewOnThread(world.key())
                            .get(SpawnKeys.WORLD_SPAWN_ROTATION)
                            .ifPresent(loginEvent::setToRotation);
                } catch (final Exception e) {
                    //
                }
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onRespawn(final RespawnPlayerEvent.SelectWorld event, @Getter("entity") final ServerPlayer player) {

        final GlobalSpawnConfig sc = this.spawnConfig.getGlobalSpawn();

        // Get the world.
        if (sc.isOnRespawn()) {
            sc.getWorld().ifPresent(event::setDestinationWorld);
        }
    }

    @Listener
    public void onRespawn(final RespawnPlayerEvent.Recreate event, @Getter("recreatedPlayer") final ServerPlayer player) {
        if (event.isBedSpawn() && !this.spawnConfig.isRedirectBedSpawn()) {
            // Nope, we don't care.
            return;
        }

        if (!event.originalDestinationPosition().equals(event.destinationPosition())) {
            // Something else has made a change, we do not.
            return;
        }

        event.setDestinationPosition(SpawnListener.process(event.destinationWorld().properties().spawnPosition()));

        // Set rotation.
        this.serviceCollection.storageManager()
                .getWorldService()
                .get(event.destinationWorld().key())
                .thenAccept(x -> {
                    x.flatMap(y -> y.get(SpawnKeys.WORLD_SPAWN_ROTATION)).ifPresent(y -> {
                        Sponge.server().scheduler().executor(this.serviceCollection.pluginContainer())
                                .execute(() -> player.setRotation(y));
                    });
                });
    }

    @Override public void onReload(final INucleusServiceCollection serviceCollection) {
        this.spawnConfig = serviceCollection.configProvider().getModuleConfig(SpawnConfig.class);
        this.checkSponge = serviceCollection.configProvider().getCoreConfig().isCheckFirstDatePlayed();
    }

    private static ServerLocation process(final ServerLocation v3d) {
        return ServerLocation.of(v3d.world(), SpawnListener.process(v3d.position()));
    }
    private static Vector3d process(final Vector3i v3i) {
        return SpawnListener.process(v3i.toDouble());
    }

    private static Vector3d process(final Vector3d v3d) {
        return v3d.floor().add(0.5d, 0, 0.5d);
    }
}
