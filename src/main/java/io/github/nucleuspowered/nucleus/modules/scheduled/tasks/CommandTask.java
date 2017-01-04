/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.scheduled.tasks;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.internal.scheduled.ScheduledTask;
import io.github.nucleuspowered.nucleus.util.DelayedTimeValue;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Task;

public class CommandTask extends ScheduledTask {

    private String commandToRun;
    @Inject
    private Game game;

    public CommandTask(String name, String commandToRun, DelayedTimeValue requeueValue) {
        super(name, requeueValue);
        this.commandToRun = commandToRun;
    }

    @Override
    protected void runScheduledTask(Task task) {
        Sponge.getCommandManager().process(game.getServer().getConsole(), commandToRun);
    }
}
