/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.scheduled;

import com.cronutils.model.time.ExecutionTime;
import io.github.nucleuspowered.nucleus.util.DelayedTimeValue;
import io.github.nucleuspowered.nucleus.util.TimeValue;
import io.github.nucleuspowered.nucleus.util.WarningTimeList;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.Task;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Create a Scheduled Task with a warning before
 */
public abstract class WarnedScheduledTask extends ScheduledTask {

    private WarningTimeList warningTimes;
    private int warningsGiven;
    private boolean shouldRunCode;

    /**
     * Construct a Scheduled Task with a Warning
     *
     * @param name
     *  The name of this task
     * @param warningTimes
     *  The list of warning times to warn beforehand
     * @param delayedTimeValue
     *  The delayed time value to schedule on
     */
    public WarnedScheduledTask(@Nonnull String name, @Nonnull WarningTimeList warningTimes,
                               @Nonnull DelayedTimeValue delayedTimeValue) {
        super(name, delayedTimeValue);
        this.warningTimes = warningTimes;
        this.warningsGiven = 0;
        this.shouldRunCode = false;
    }

    abstract void runWarning(Task task, long timeToNextExecution);

    /**
     * Custom Requeue logic for WarnedScheduledTask. For warnings _we must_ control the requeue logic here
     *
     * @param task
     *  The task to requeue
     */
    @Override
    void requeue(Task task) {
        if (task.getInterval() != 0) {
            // We have to control requeues for warning.
            task.cancel();
        }
        // Zero-Indexed Array, when the sizes are equal, we've run the last warning.
        if (this.warningsGiven == this.warningTimes.getAmountOfWarnings()) {
            if (!shouldRunCode) {
                this.shouldRunCode = true;
                this.queueWithDelayMillis(this.timeToNextExecution(), task);
            } else {
                this.warningsGiven = 0;
                this.shouldRunCode = false;
                this.queueWithDelayMillis(this.getInitialDelay(), task);
            }
        }
        TimeValue timeToWarnBefore = this.warningTimes.getWarningTimeAt(this.warningsGiven);
        this.warningsGiven++;
        this.queueWithDelayMillis(this.timeToNextExecution() - timeToWarnBefore.getAsMillis(), task);
    }

    /**
     * Helper method to queue a job with a specific millis delay
     *
     * @param delay
     *  The millis to delay
     * @param task
     *  The task to queue
     */
    private void queueWithDelayMillis(long delay, Task task) {
        Scheduler scheduler = Sponge.getScheduler();
        Task.Builder builder = scheduler.createTaskBuilder()
                .execute(this).delay(delay, TimeUnit.MILLISECONDS).name(task.getName());

        if (task.isAsynchronous()) {
            builder.async();
        }

        builder.submit(task.getOwner());
    }

    /**
     * @return
     *  The raw time to next execution from the cron without any warning times attached
     */
    private long timeToNextExecution() {
        Duration timeToNextExecution;
        try {
            ExecutionTime et = ExecutionTime.forCron(this.delayedTimeValue.getAsCron());
            timeToNextExecution = et.timeToNextExecution(ZonedDateTime.now());
        } catch (Exception e1) {
            return -1;
        }
        return timeToNextExecution.toMillis();
    }

    /**
     * @return
     *  The initial delay for this job
     */
    @Override
    public long getInitialDelay() {
        long timeToNextExecution = timeToNextExecution();
        if (timeToNextExecution == -1) {
            return -1;
        }
        long maxDelayTime = this.warningTimes.getWarningTimeAt(this.warningTimes.getAmountOfWarnings() - 1)
                .getAsMillis();
        long hopingToReturn = timeToNextExecution - maxDelayTime;
        if (hopingToReturn < 0) {
            hopingToReturn = 0;
        }
        return hopingToReturn;
    }

    /**
     * Run the jobs "cycle" e.g. run a warning, passing in the time to next execution, or run the task
     *
     * @param task
     *  The task to run
     */
    @Override
    public void accept(Task task) {
        if (this.isCancelled) {
            if (task.getInterval() > 0) {
                task.cancel();
            }
            return;
        }
        if (this.shouldRunCode) {
            this.runScheduledTask(task);
        } else {
            this.runWarning(task, this.timeToNextExecution());
        }
        this.requeue(task);
    }
}
