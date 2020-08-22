/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.afk.listeners;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.modules.afk.config.AFKConfig;
import io.github.nucleuspowered.nucleus.modules.afk.services.AFKHandler;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.filter.cause.Root;

import java.util.List;

import javax.inject.Inject;

public class AFKCommandListener extends AbstractAFKListener implements ListenerBase.Conditional {
    private final List<String> commands = Lists.newArrayList("afk", "away");

    @Inject
    public AFKCommandListener(INucleusServiceCollection serviceCollection) {
        super(serviceCollection.getServiceUnchecked(AFKHandler.class));
    }

    @Listener
    public void onPlayerCommand(final SendCommandEvent event, @Root Player player) {
        // Did the subject run /afk? Then don't do anything, we'll toggle it
        // anyway.
        if (!this.commands.contains(event.getCommand().toLowerCase())) {
            update(player);
        }
    }

    @Override
    public boolean shouldEnable(INucleusServiceCollection serviceCollection) {
        return serviceCollection.moduleDataProvider().getModuleConfig(AFKConfig.class)
                .getTriggers()
                .isOnCommand();
    }
}
