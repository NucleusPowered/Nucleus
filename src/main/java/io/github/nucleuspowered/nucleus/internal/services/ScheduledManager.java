/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.services;

import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.api.service.NucleusScheduledManagerService;
import io.github.nucleuspowered.nucleus.internal.scheduled.ScheduledTask;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Task;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.concurrent.ThreadSafe;

/**
 * The scheduled manager
 */
@ThreadSafe
public class ScheduledManager implements NucleusScheduledManagerService {

    private Map<String, ScheduledTask> tasks;
    private NucleusPlugin plugin;

    /**
     * Construct a Scheduled Manager
     *
     * @param plugin
     *  The nucleus plugin to inject scheduled tasks
     */
    public ScheduledManager(NucleusPlugin plugin) {
        this.tasks = new ConcurrentHashMap<>();
        this.plugin = plugin;
    }

    @Override
    public void addScheduledTask(ScheduledTask task) throws IllegalArgumentException {
        this.addScheduledTask(task, true);
    }

    @Override
    public void addScheduledTask(ScheduledTask task, boolean async) throws IllegalArgumentException {
        if (this.tasks.containsKey(task.getName())) {
            throw new IllegalArgumentException("Task already exists");
        }
        plugin.getInjector().injectMembers(task);
        this.tasks.put(task.getName(), task);
        Task.Builder builder = Sponge.getScheduler().createTaskBuilder().execute(task)
                .delay(task.getInitialDelay(), TimeUnit.MILLISECONDS).name(task.getName());
        if (async) {
            builder.async();
        }
        builder.submit(plugin);
    }

    @Override
    public List<ScheduledTask> getScheduledTasks() {
        return this.tasks.values().stream().collect(Collectors.toList());
    }

    @Override
    public Optional<ScheduledTask> getTaskByName(String name) {
        return Optional.ofNullable(this.tasks.get(name));
    }
}
