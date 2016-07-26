/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warn.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.data.WarnData;
import io.github.nucleuspowered.nucleus.internal.ListenerBase;
import io.github.nucleuspowered.nucleus.modules.warn.config.WarnConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.warn.handlers.WarnHandler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WarnListener extends ListenerBase {

    @Inject private WarnHandler handler;
    @Inject private WarnConfigAdapter wca;

    /**
     * At the time the player joins, check to see if the player has been warned.
     *
     * @param event The event.
     */
    @Listener
    public void onPlayerLogin(final ClientConnectionEvent.Join event) {
        Sponge.getScheduler().createTaskBuilder().async().delay(500, TimeUnit.MILLISECONDS).execute(() -> {
            Player player = event.getTargetEntity();
            List<WarnData> warnings = handler.getWarnings(player);
            if (warnings != null) {
                for (WarnData warning : warnings) {
                    if (warning.isExpired()) {
                        return;
                    }
                    warning.nextLoginToTimestamp();

                    if (warning.getEndTimestamp().isPresent() && warning.getEndTimestamp().get().isBefore(Instant.now())) {
                        handler.removeWarning(player, warning);
                    } else {
                        if (wca.getNodeOrDefault().isShowOnLogin()) {
                            if (warning.getEndTimestamp().isPresent()) {
                                player.sendMessage(Util.getTextMessageWithFormat("warn.playernotify.time", warning.getReason(),
                                        Util.getTimeStringFromSeconds(Instant.now().until(warning.getEndTimestamp().get(), ChronoUnit.SECONDS))));
                            } else {
                                player.sendMessage(Util.getTextMessageWithFormat("warn.playernotify.standard", warning.getReason()));
                            }
                        }
                    }
                }
            }
        }).submit(plugin);
    }
}
