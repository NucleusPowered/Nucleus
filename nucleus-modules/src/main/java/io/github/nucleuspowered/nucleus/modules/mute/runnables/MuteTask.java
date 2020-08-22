/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mute.runnables;

import io.github.nucleuspowered.nucleus.datatypes.EndTimestamp;
import io.github.nucleuspowered.nucleus.modules.mute.services.MuteHandler;
import io.github.nucleuspowered.nucleus.scaffold.task.TaskBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Task;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import javax.inject.Inject;

public class MuteTask implements TaskBase {

    private final MuteHandler muteHandler;

    @Inject
    public MuteTask(INucleusServiceCollection serviceCollection) {
        this.muteHandler = serviceCollection.getServiceUnchecked(MuteHandler.class);
    }

    @Override
    public void accept(Task task) {
        Sponge.getServer()
                .getOnlinePlayers()
                .stream()
                .filter(this.muteHandler::isMutedCached)
                .filter(x -> this.muteHandler.getPlayerMuteData(x).map(EndTimestamp::expired).orElse(false))
                .forEach(this.muteHandler::unmutePlayer);
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public Duration interval() {
        return Duration.of(1, ChronoUnit.SECONDS);
    }

}
