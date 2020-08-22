/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.connectionmessages.listeners;

import com.google.common.collect.ImmutableMap;
import io.github.nucleuspowered.nucleus.api.core.event.NucleusFirstJoinEvent;
import io.github.nucleuspowered.nucleus.modules.connectionmessages.ConnectionMessagesPermissions;
import io.github.nucleuspowered.nucleus.modules.connectionmessages.config.ConnectionMessagesConfig;
import io.github.nucleuspowered.nucleus.modules.core.CoreKeys;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.services.interfaces.IStorageManager;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;

import java.util.Optional;

import javax.inject.Inject;

public class ConnectionMessagesListener implements IReloadableService.Reloadable, ListenerBase {

    private final IStorageManager storageManager;
    private final IPermissionService permissionService;
    private final PluginContainer pluginContainer;

    private ConnectionMessagesConfig cmc = new ConnectionMessagesConfig();

    @Inject
    public ConnectionMessagesListener(INucleusServiceCollection serviceCollection) {
        this.permissionService = serviceCollection.permissionService();
        this.storageManager = serviceCollection.storageManager();
        this.pluginContainer = serviceCollection.pluginContainer();
    }

    @Listener
    public void onPlayerLogin(ClientConnectionEvent.Join joinEvent, @Getter("getTargetEntity") Player pl) {
        if (joinEvent.isMessageCancelled() || (this.cmc.isDisableWithPermission() &&
                this.permissionService.hasPermission(pl, ConnectionMessagesPermissions.CONNECTIONMESSSAGES_DISABLE))) {
            joinEvent.setMessageCancelled(true);
            return;
        }

        try {
            Optional<String> lastKnown = storageManager.getUserOnThread(pl.getUniqueId()).flatMap(x -> x.get(CoreKeys.LAST_KNOWN_NAME));
            if (this.cmc.isDisplayPriorName() &&
                !this.cmc.getPriorNameMessage().isEmpty() &&
                !lastKnown.orElseGet(pl::getName).equalsIgnoreCase(pl.getName())) {
                    // Name change!
                    joinEvent.getChannel().orElse(MessageChannel.TO_ALL).send(this.pluginContainer,
                            this.cmc.getPriorNameMessage()
                                    .getForCommandSource(pl,
                                            ImmutableMap.of("previousname", cs -> Optional.of(Text.of(lastKnown.get())))));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (this.cmc.isModifyLoginMessage()) {
            if (this.cmc.getLoginMessage().isEmpty()) {
                joinEvent.setMessageCancelled(true);
            } else {
                joinEvent.setMessage(this.cmc.getLoginMessage().getForCommandSource(pl));
            }
        }
    }

    @Listener
    public void onPlayerFirstJoin(NucleusFirstJoinEvent event, @Getter("getTargetEntity") Player pl) {
        if (this.cmc.isShowFirstTimeMessage() && !this.cmc.getFirstTimeMessage().isEmpty()) {
            event.getChannel().orElse(MessageChannel.TO_ALL).send(this.pluginContainer, this.cmc.getFirstTimeMessage().getForCommandSource(pl));
        }
    }

    @Listener
    public void onPlayerQuit(ClientConnectionEvent.Disconnect leaveEvent, @Getter("getTargetEntity") Player pl) {
        if (leaveEvent.isMessageCancelled() || (this.cmc.isDisableWithPermission() &&
                this.permissionService.hasPermission(pl, ConnectionMessagesPermissions.CONNECTIONMESSSAGES_DISABLE))) {
            leaveEvent.setMessageCancelled(true);
            return;
        }

        if (this.cmc.isModifyLogoutMessage()) {
            if (this.cmc.getLogoutMessage().isEmpty()) {
                leaveEvent.setMessageCancelled(true);
            } else {
                leaveEvent.setMessage(this.cmc.getLogoutMessage().getForCommandSource(pl));
            }
        }
    }

    @Override public void onReload(INucleusServiceCollection serviceCollection) {
        this.cmc = serviceCollection.moduleDataProvider().getModuleConfig(ConnectionMessagesConfig.class);
    }
}
