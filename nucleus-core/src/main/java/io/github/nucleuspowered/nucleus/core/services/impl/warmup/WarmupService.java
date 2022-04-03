/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.warmup;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.nucleuspowered.nucleus.core.core.config.WarmupConfig;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IWarmupService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.plugin.PluginContainer;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Singleton
public class WarmupService implements IWarmupService, IReloadableService.Reloadable {

    private final Object lockingObject = new Object();

    private final PluginContainer pluginContainer;
    private final IMessageProviderService messageProviderService;
    private WarmupConfig warmupConfig = new WarmupConfig();

    // player to task
    private final BiMap<UUID, UUID> tasks = HashBiMap.create();

    // task to warmup
    private final BiMap<UUID, WarmupTask> uuidToWarmup = HashBiMap.create();

    @Inject
    public WarmupService(final PluginContainer pluginContainer, final IMessageProviderService messageProviderService, final IReloadableService reloadableService) {
        this.pluginContainer = pluginContainer;
        this.messageProviderService = messageProviderService;
        reloadableService.registerReloadable(this);
    }

    @Override
    public void executeAfter(final Player target, final Duration duration, final WarmupTask runnable) {
        this.executeAfter(target, duration, runnable, false);
    }

    @Override public void executeAfter(final Player target, final Duration duration, final WarmupTask runnable, final boolean sendMessage) {
         synchronized (this.lockingObject) {
            this.cancelInternal(target);

            if (sendMessage) {
                this.messageProviderService.sendMessageTo(target, "warmup.start",
                        this.messageProviderService.getTimeString(target.locale(), duration));
                if (this.warmupConfig.isOnCommand() && this.warmupConfig.isOnMove()) {
                    this.messageProviderService.sendMessageTo(target, "warmup.both");
                } else if (this.warmupConfig.isOnCommand()) {
                    this.messageProviderService.sendMessageTo(target, "warmup.onCommand");
                } else if (this.warmupConfig.isOnMove()) {
                    this.messageProviderService.sendMessageTo(target, "warmup.onMove");
                }
            }

            // build the task
            final UUID playerTarget = target.uniqueId();
            final Consumer<ScheduledTask> taskToSubmit = (ScheduledTask task) -> {
                this.tasks.remove(playerTarget);
                this.uuidToWarmup.remove(task.uniqueId());

                if (Sponge.server().player(playerTarget).isPresent()) {
                    // Only run if the player is still on the server.
                    runnable.run();
                }
            };

            final Task t = Task.builder()
                    .delay(duration.toMillis(), TimeUnit.MILLISECONDS)
                    .execute(taskToSubmit)
                    .plugin(this.pluginContainer)
                    .build();
            final ScheduledTask scheduledTask = Sponge.server().scheduler().submit(t, "Nucleus Warmup task: " + playerTarget.toString());
            this.tasks.put(playerTarget, scheduledTask.uniqueId());
            this.uuidToWarmup.put(scheduledTask.uniqueId(), runnable);
        }
    }

    @Override public boolean cancel(final Player player) {
        synchronized (this.lockingObject) {
            return this.cancelInternal(player);
        }
    }

    private boolean cancelInternal(final Player player) {
        final UUID taskUUID = this.tasks.get(player.uniqueId());
        if (taskUUID != null) {
            Sponge.server().scheduler().findTask(taskUUID).ifPresent(ScheduledTask::cancel);
            final WarmupTask task = this.uuidToWarmup.get(taskUUID);
            if (task != null) {
                // if we get here, it was never cancelled.
                task.onCancel();
            }

            this.uuidToWarmup.remove(taskUUID);
        }
        this.tasks.remove(player.uniqueId());
        return taskUUID != null;
    }

    @Override public boolean awaitingExecution(final Player player) {
        synchronized (this.lockingObject) {
            final UUID taskUUID = this.tasks.get(player.uniqueId());
            if (taskUUID != null) {
                if (Sponge.server().scheduler().findTask(taskUUID).isPresent()) {
                    // remove entries
                    return true;
                } else {
                    this.uuidToWarmup.remove(taskUUID);
                }
            }
            this.tasks.remove(player.uniqueId());
            return false;
        }
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        this.warmupConfig = serviceCollection.configProvider().getCoreConfig().getWarmupConfig();
    }
}
