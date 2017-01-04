/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.service;

import io.github.nucleuspowered.nucleus.internal.scheduled.ScheduledTask;

import java.util.List;
import java.util.Optional;

/**
 * A Manager of Scheduled Tasks from Nucleus
 */
public interface NucleusScheduledManagerService {

    /**
     * Adds a scheduled task if a task with the same name hasn't been queued, will set isAsync to true
     *
     * @param task
     *  The ScheduledTask to add
     * @throws IllegalArgumentException
     *  Throws an IllegalArgumentException if a task with the same name has been queued
     */
    void addScheduledTask(ScheduledTask task) throws IllegalArgumentException;

    /**
     * Adds a scheduled task manually specifying isAsync
     *
     * @param task
     *  The ScheduledTask to add
     * @param isAsync
     *  Manually specify if this job should run async
     * @throws IllegalArgumentException
     *  Throws an IllegalArgumentException if a task with the same name has been queued
     */
    void addScheduledTask(ScheduledTask task, boolean isAsync) throws IllegalArgumentException;

    /**
     * @return
     *  All the scheduled tasks which have been queued
     */
    List<ScheduledTask> getScheduledTasks();

    /**
     * Get a task by name
     *
     * @param name
     *  The name of the task to get
     * @return
     *  The scheduled task if its there
     */
    Optional<ScheduledTask> getTaskByName(String name);
}
