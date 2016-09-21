/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.info.listeners;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.ChatUtil;
import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.listeners.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.info.InfoModule;
import io.github.nucleuspowered.nucleus.modules.info.commands.MotdCommand;
import io.github.nucleuspowered.nucleus.modules.info.config.InfoConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.info.handlers.InfoHelper;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class InfoListener extends ListenerBase {

    @Inject private ChatUtil chatUtil;
    @Inject private PermissionRegistry pr;
    @Inject private InfoConfigAdapter ica;

    private String motdPermission = null;

    @Listener
    public void playerJoin(ClientConnectionEvent.Join event) {
        if (!ica.getNodeOrDefault().isShowMotdOnJoin()) {
            return;
        }

        final Player player = event.getTargetEntity();

        // Send message one second later on the Async thread.
        Sponge.getScheduler().createAsyncExecutor(plugin).schedule(() -> {
                if (player.hasPermission(getMotdPermission())) {
                    plugin.getTextFileController(InfoModule.MOTD_KEY).ifPresent(x -> InfoHelper.sendInfo(x, player, chatUtil, ica.getNodeOrDefault().getMotdTitle()));
                }
            }, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    public Map<String, PermissionInformation> getPermissions() {
        Map<String, PermissionInformation> msp = Maps.newHashMap();
        msp.put(getMotdPermission(), new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.motd.join"), SuggestedLevel.USER));
        return msp;
    }

    private String getMotdPermission() {
        if (motdPermission == null) {
            motdPermission = pr.getService(MotdCommand.class).orElse(new CommandPermissionHandler(new MotdCommand(), plugin)).getPermissionWithSuffix("login");
        }

        return motdPermission;
    }
}
