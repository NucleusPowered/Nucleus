/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.listeners;

import io.github.nucleuspowered.nucleus.modules.world.WorldKeys;
import io.github.nucleuspowered.nucleus.modules.world.services.WorldHelper;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IWorldDataObject;
import org.spongepowered.api.GameState;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.World;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class WorldGenListener implements ListenerBase {

    private final INucleusServiceCollection serviceCollection;

    @Inject
    public WorldGenListener(INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
    }

    @Listener
    public void onStart(GameStartedServerEvent event) {
        Task.builder().execute(() -> Sponge.getServer().getWorlds().forEach(this::onWorldLoad)).delay(1, TimeUnit.SECONDS).submit(this.serviceCollection.pluginContainer());
    }

    @Listener
    public void onWorldLoad(LoadWorldEvent event) {
        if (Sponge.getGame().getState() == GameState.SERVER_STARTED) {
            Task.builder().execute(() -> onWorldLoad(event.getTargetWorld())).delay(1, TimeUnit.SECONDS).submit(this.serviceCollection.pluginContainer());
        }
    }

    private void onWorldLoad(final World world) {
        WorldHelper worldHelper = this.serviceCollection.getServiceUnchecked(WorldHelper.class);
        final CompletableFuture<Optional<IWorldDataObject>> cfo =
                this.serviceCollection.storageManager().getWorldService().get(world.getUniqueId());

        // avoiding main thread loading
        Task.builder()
                .delay(500, TimeUnit.MILLISECONDS)
                .interval(500, TimeUnit.MILLISECONDS)
                .execute(task -> {
                    try {
                        if (cfo.isDone()) {
                            task.cancel();
                            if (!cfo.isCompletedExceptionally()) {
                                // We know there isn't an exception.
                                Optional<IWorldDataObject> optionalWorldDataObject = cfo.join();
                                if (optionalWorldDataObject.isPresent()) {
                                    IWorldDataObject worldDataObject = optionalWorldDataObject.get();
                                    boolean act = worldDataObject.get(WorldKeys.WORLD_PREGEN_START).orElse(false);
                                    if (act && worldHelper.startPregenningForWorld(
                                            world,
                                            worldDataObject.getOrDefault(WorldKeys.WORLD_PREGEN_AGGRESSIVE),
                                            worldDataObject.getOrDefault(WorldKeys.WORLD_PREGEN_SAVE_FREQUENCY),
                                            worldDataObject.getOrDefault(WorldKeys.WORLD_PREGEN_TICK_PERCENT),
                                            worldDataObject.getOrDefault(WorldKeys.WORLD_PREGEN_TICK_FREQUENCY),
                                            true
                                    )) {
                                        this.serviceCollection.messageProvider()
                                                .sendMessageTo(Sponge.getServer().getConsole(), "command.world.gen.started", world.getName());
                                    }
                                }
                            }
                        }
                    } catch (IllegalStateException e) {
                        this.serviceCollection.logger().error(
                                "Could not determine World Generation restart status for world {}, WorldProperties could not be loaded from the Sponge "
                                        + "World Manager (Sponge.getServer().getWorldProperties({}) returned empty.)", world.getName(), world.getUniqueId());
                        e.printStackTrace();
                    }
                }).submit(this.serviceCollection.pluginContainer());
    }
}
