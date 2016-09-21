/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warn.listeners;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.data.WarnData;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.listeners.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.warn.config.WarnConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.warn.handlers.WarnHandler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.MutableMessageChannel;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class WarnListener extends ListenerBase {

    @Inject private WarnHandler handler;
    @Inject private WarnConfigAdapter wca;

    private final String showOnLogin = PermissionRegistry.PERMISSIONS_PREFIX + "note.showonlogin";

    /**
     * At the time the player joins, check to see if the player has been warned.
     *
     * @param event The event.
     */
    @Listener
    public void onPlayerLogin(final ClientConnectionEvent.Join event) {
        Sponge.getScheduler().createTaskBuilder().async().delay(500, TimeUnit.MILLISECONDS).execute(() -> {
            Player player = event.getTargetEntity();
            List<WarnData> warnings = handler.getWarnings(player, true, false);
            if (warnings != null && !warnings.isEmpty()) {
                for (WarnData warning : warnings) {
                    warning.nextLoginToTimestamp();

                    if (warning.getEndTimestamp().isPresent() && warning.getEndTimestamp().get().isBefore(Instant.now())) {
                        handler.removeWarning(player, warning);
                    } else {
                        if (wca.getNodeOrDefault().isShowOnLogin()) {
                            if (warning.getEndTimestamp().isPresent()) {
                                player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("warn.playernotify.time", warning.getReason(),
                                        Util.getTimeStringFromSeconds(Instant.now().until(warning.getEndTimestamp().get(), ChronoUnit.SECONDS))));
                            } else {
                                player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("warn.playernotify.standard", warning.getReason()));
                            }
                        }
                    }
                }

                // Now, let's check again
                if (wca.getNodeOrDefault().isShowOnLogin()) {
                    List<WarnData> lwd = warnings.stream().filter(x -> !x.isExpired()).collect(Collectors.toList());
                    if (!lwd.isEmpty()) {
                        MutableMessageChannel messageChannel = MessageChannel.permission(showOnLogin).asMutable();
                        messageChannel.send(plugin.getMessageProvider().getTextMessageWithFormat("warn.login.notify", player.getName(), String.valueOf(lwd.size())).toBuilder()
                                .onHover(TextActions.showText(plugin.getMessageProvider().getTextMessageWithFormat("warn.login.view", player.getName())))
                                .onClick(TextActions.runCommand("/checkwarnings " + player.getName()))
                                .build());
                    }
                }
            }
        }).submit(plugin);
    }

    @Override
    public Map<String, PermissionInformation> getPermissions() {
        Map<String, PermissionInformation> mp = Maps.newHashMap();
        mp.put(showOnLogin, new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.warn.showonlogin"), SuggestedLevel.MOD));
        return mp;
    }
}
