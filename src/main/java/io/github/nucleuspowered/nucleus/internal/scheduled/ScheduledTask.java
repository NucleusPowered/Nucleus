/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.scheduled;

import com.cronutils.model.time.ExecutionTime;
import io.github.nucleuspowered.nucleus.util.DelayedTimeValue;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.Task;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A ScheduledTask, used to help in scenarios of "Real World Time" where the interval isn't constant
 */
public abstract class ScheduledTask implements Consumer<Task> {

    @Nullable
    protected DelayedTimeValue delayedTimeValue;
    protected boolean isCancelled;
    protected String name;

    /**
     * Creates a ScheduledTask
     *
     * @param name
     *  The name of this scheduled task, should be unique
     * @param delayedTimeValue
     *  The potential delayed time value for this scheduled task
     */
    public ScheduledTask(@Nonnull String name, @Nullable DelayedTimeValue delayedTimeValue) {
        this.delayedTimeValue = delayedTimeValue;
        this.isCancelled = false;
        this.name = name;
    }

    /**
     * @return
     *  The name of this scheduled task
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the cancelled state
     *
     * @param state
     *  The state to set cancelled too
     */
    public void setCancelled(boolean state) {
        this.isCancelled = state;
    }

    /**
     * @return
     *  If this job is cancelled
     */
    public boolean isCancelled() {
        return this.isCancelled;
    }

    /**
     * Runs the actual scheduled task
     *
     * @param task
     *  The task object
     */
    protected abstract void runScheduledTask(Task task);

    /**
     * Requeues the particular task
     *
     * @param task
     *  The task to requeue
     */
    void requeue(Task task) {
        if (task.getInterval() <= 0 && this.delayedTimeValue != null) {
            if (this.delayedTimeValue.isSingle()) {
                return;
            }
            try {
                Scheduler scheduler = Sponge.getScheduler();
                Task.Builder builder = scheduler.createTaskBuilder().execute(this);
                ExecutionTime et = ExecutionTime.forCron(this.delayedTimeValue.getAsCron());
                Duration timeToNextExecution = et.timeToNextExecution(ZonedDateTime.now());
                builder.delay(timeToNextExecution.toMillis(), TimeUnit.MILLISECONDS).name(task.getName());

                if (task.isAsynchronous()) {
                    builder.async();
                }

                builder.submit(task.getOwner());
            } catch (Exception e1) {
                // No possible requeue time.
            }
        }
    }

    /**
     * @return
     *  The initial delay to wait for queueing
     *  Allows us to have things like ScheduledTasks with warnings before
     */
    public long getInitialDelay() {
        if (this.delayedTimeValue != null) {
            try {
                ExecutionTime et = ExecutionTime.forCron(this.delayedTimeValue.getAsCron());
                Duration timeToNextExecution = et.timeToNextExecution(ZonedDateTime.now());
                return timeToNextExecution.toMillis();
            } catch (Exception e1) {
                //No Next Queue time.
            }
        }
        return -1;
    }

    @Override
    public void accept(Task task) {
        if (this.isCancelled) {
            if (task.getInterval() > 0) {
                task.cancel();
            }
            return;
        }
        this.runScheduledTask(task);
        this.requeue(task);
    }
}
