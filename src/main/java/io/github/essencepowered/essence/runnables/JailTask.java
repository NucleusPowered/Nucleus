/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.runnables;

import io.github.essencepowered.essence.Essence;
import io.github.essencepowered.essence.Util;
import io.github.essencepowered.essence.api.PluginModule;
import io.github.essencepowered.essence.internal.TaskBase;
import io.github.essencepowered.essence.internal.annotations.Modules;
import io.github.essencepowered.essence.internal.services.datastore.UserConfigLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;

@Modules(PluginModule.JAILS)
public class JailTask extends TaskBase {
    @Inject
    private Essence plugin;

    @Override
    public void accept(Task task) {
        Collection<Player> pl = Sponge.getServer().getOnlinePlayers();
        UserConfigLoader ucl = plugin.getUserLoader();
        pl.stream().map(x -> {
            try {
                return ucl.getUser(x);
            } catch (IOException | ObjectMappingException e) {
                e.printStackTrace();
                return null;
            }
        }).filter(x -> x == null || x.getJailData().isPresent()).forEach(x -> Util.testForEndTimestamp(x.getJailData(), () -> plugin.getJailHandler().unjailPlayer(x.getUser())));
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public int secondsPerRun() {
        return 2;
    }
}
