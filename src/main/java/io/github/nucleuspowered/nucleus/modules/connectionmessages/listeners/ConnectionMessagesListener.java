/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.connectionmessages.listeners;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.events.NucleusFirstJoinEvent;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.interfaces.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.internal.traits.IDataManagerTrait;
import io.github.nucleuspowered.nucleus.modules.connectionmessages.config.ConnectionMessagesConfig;
import io.github.nucleuspowered.nucleus.modules.connectionmessages.config.ConnectionMessagesConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.core.CoreKeys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ConnectionMessagesListener implements Reloadable, ListenerBase, IDataManagerTrait {

    private ConnectionMessagesConfig cmc = new ConnectionMessagesConfig();
    private final String disablePermission = PermissionRegistry.PERMISSIONS_PREFIX + "connectionmessages.disable";

    @Override
    public Map<String, PermissionInformation> getPermissions() {
        return new HashMap<String, PermissionInformation>() {{
            put(ConnectionMessagesListener.this.disablePermission, new PermissionInformation(
                    Nucleus.getNucleus().getMessageProvider().getMessageWithFormat("permission.connectionmesssages.disable"),
                SuggestedLevel.NONE));
        }};
    }

    @Listener
    public void onPlayerLogin(ClientConnectionEvent.Join joinEvent, @Getter("getTargetEntity") Player pl) {
        if (joinEvent.isMessageCancelled() || (this.cmc.isDisableWithPermission() && hasPermission(pl, this.disablePermission))) {
            joinEvent.setMessageCancelled(true);
            return;
        }

        try {
            Optional<String> lastKnown = getUserOnThread(pl.getUniqueId()).flatMap(x -> x.get(CoreKeys.LAST_KNOWN_NAME));
            if (this.cmc.isDisplayPriorName() &&
                !this.cmc.getPriorNameMessage().isEmpty() &&
                !lastKnown.orElseGet(pl::getName).equalsIgnoreCase(pl.getName())) {
                    // Name change!
                    joinEvent.getChannel().orElse(MessageChannel.TO_ALL).send(Nucleus.getNucleus(),
                            this.cmc.getPriorNameMessage()
                                    .getForCommandSource(pl,
                                            ImmutableMap.of("previousname", cs -> Optional.of(Text.of(lastKnown.get()))), Maps.newHashMap()));
            }
        } catch (Exception e) {
            if (Nucleus.getNucleus().isDebugMode()) {
                e.printStackTrace();
            }
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
            event.getChannel().orElse(MessageChannel.TO_ALL).send(Nucleus.getNucleus(), this.cmc.getFirstTimeMessage().getForCommandSource(pl));
        }
    }

    @Listener
    public void onPlayerQuit(ClientConnectionEvent.Disconnect leaveEvent, @Getter("getTargetEntity") Player pl) {
        if (leaveEvent.isMessageCancelled() || (this.cmc.isDisableWithPermission() && hasPermission(pl, this.disablePermission))) {
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

    @Override
    public void onReload() {
        this.cmc = Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(ConnectionMessagesConfigAdapter.class).getNodeOrDefault();
    }
}
