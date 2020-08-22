/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.afk.listeners;

import io.github.nucleuspowered.nucleus.modules.afk.config.AFKConfig;
import io.github.nucleuspowered.nucleus.modules.afk.services.AFKHandler;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import org.spongepowered.api.entity.living.player.Player;

import java.util.function.Predicate;

abstract class AbstractAFKListener implements ListenerBase {

    private final AFKHandler handler;

    protected AbstractAFKListener(AFKHandler handler) {
        this.handler = handler;
    }

    final void update(Player player) {
        this.handler.stageUserActivityUpdate(player);
    }

    final boolean getTriggerConfigEntry(AFKConfig config, Predicate<AFKConfig.Triggers> triggersPredicate) {
        return triggersPredicate.test(config.getTriggers());
    }
}
