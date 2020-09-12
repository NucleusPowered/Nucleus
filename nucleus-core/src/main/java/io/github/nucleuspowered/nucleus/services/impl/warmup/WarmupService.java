/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.warmup;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.nucleuspowered.nucleus.services.interfaces.IWarmupService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Singleton
public class WarmupService implements IWarmupService {

    private final Object lockingObject = new Object();

    private final PluginContainer pluginContainer;

    // player to task
    private final BiMap<UUID, UUID> tasks = HashBiMap.create();

    // task to warmup
    private final BiMap<UUID, WarmupTask> uuidToWarmup = HashBiMap.create();

    @Inject
    public WarmupService(PluginContainer pluginContainer) {
        this.pluginContainer = pluginContainer;
    }

    @Override public void executeAfter(Player target, Duration duration, WarmupTask runnable) {
        execute(target, duration, runnable, false);
    }

    @Override public void executeAfterAsync(Player target, Duration duration, WarmupTask runnable) {
        execute(target, duration, runnable, true);
    }

    private void execute(Player target, Duration duration, WarmupTask runnable, boolean async) {
        synchronized (this.lockingObject) {
            cancelInternal(target);

            // build the task
            final UUID playerTarget = target.getUniqueId();
            Consumer<Task> taskToSubmit = task -> {
                this.tasks.remove(playerTarget);
                this.uuidToWarmup.remove(task.getUniqueId());

                if (Sponge.getServer().getPlayer(playerTarget).isPresent()) {
                    // Only run if the player is still on the server.
                    runnable.run();
                }
            };

            Task.Builder builder = Task.builder()
                    .execute(taskToSubmit)
                    .delay(duration.toMillis(), TimeUnit.MILLISECONDS)
                    .name("Nucleus Warmup task: " + playerTarget.toString());
            if (async) {
                builder.async();
            }
            Task t = builder.submit(this.pluginContainer);
            this.tasks.put(playerTarget, t.getUniqueId());
            this.uuidToWarmup.put(t.getUniqueId(), runnable);
        }
    }

    @Override public boolean cancel(Player player) {
        synchronized (this.lockingObject) {
            return cancelInternal(player);
        }
    }

    private boolean cancelInternal(Player player) {
        UUID taskUUID = this.tasks.get(player.getUniqueId());
        if (taskUUID != null) {
            Sponge.getScheduler().getTaskById(taskUUID).ifPresent(Task::cancel);
            WarmupTask task = this.uuidToWarmup.get(taskUUID);
            if (task != null) {
                // if we get here, it was never cancelled.
                task.onCancel();
            }

            this.uuidToWarmup.remove(taskUUID);
        }
        this.tasks.remove(player.getUniqueId());
        return taskUUID != null;
    }

    @Override public boolean awaitingExecution(Player player) {
        synchronized (this.lockingObject) {
            UUID taskUUID = this.tasks.get(player.getUniqueId());
            if (taskUUID != null) {
                if (Sponge.getScheduler().getTaskById(taskUUID).isPresent()) {
                    // remove entries
                    return true;
                } else {
                    this.uuidToWarmup.remove(taskUUID);
                }
            }
            this.tasks.remove(player.getUniqueId());
            return false;
        }
    }
}
