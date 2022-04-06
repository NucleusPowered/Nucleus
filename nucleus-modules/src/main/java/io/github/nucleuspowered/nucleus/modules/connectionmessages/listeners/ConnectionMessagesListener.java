/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.connectionmessages.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.core.event.NucleusFirstJoinEvent;
import io.github.nucleuspowered.nucleus.api.text.NucleusTextTemplate;
import io.github.nucleuspowered.nucleus.core.core.CoreKeys;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.texttemplatefactory.NucleusTextTemplateImpl;
import io.github.nucleuspowered.nucleus.core.services.interfaces.INucleusTextTemplateFactory;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IStorageManager;
import io.github.nucleuspowered.nucleus.modules.connectionmessages.ConnectionMessagesPermissions;
import io.github.nucleuspowered.nucleus.modules.connectionmessages.config.ConnectionMessagesConfig;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;

import java.util.Collections;
import java.util.Optional;

public class ConnectionMessagesListener implements IReloadableService.Reloadable, ListenerBase {

    private final IStorageManager storageManager;
    private final IPermissionService permissionService;

    private ConnectionMessagesConfig cmc = new ConnectionMessagesConfig();
    private NucleusTextTemplate firstTimeMessage = NucleusTextTemplateImpl.empty();
    private NucleusTextTemplate loginMessage = NucleusTextTemplateImpl.empty();
    private NucleusTextTemplate logoutMessage = NucleusTextTemplateImpl.empty();
    private NucleusTextTemplate priorNameMessage = NucleusTextTemplateImpl.empty();

    @Inject
    public ConnectionMessagesListener(final INucleusServiceCollection serviceCollection) {
        this.permissionService = serviceCollection.permissionService();
        this.storageManager = serviceCollection.storageManager();
    }

    @Listener
    public void onPlayerLogin(final ServerSideConnectionEvent.Join joinEvent, @Getter("player") final ServerPlayer pl) {
        if (joinEvent.isMessageCancelled() || (this.cmc.isDisableWithPermission() &&
                this.permissionService.hasPermission(pl, ConnectionMessagesPermissions.CONNECTIONMESSSAGES_DISABLE))) {
            joinEvent.setMessageCancelled(true);
            joinEvent.setAudience(null);
            return;
        }

        try {
            final Optional<String> lastKnown = this.storageManager.getUserOnThread(pl.uniqueId()).flatMap(x -> x.get(CoreKeys.LAST_KNOWN_NAME));
            if (this.cmc.isDisplayPriorName() &&
                !this.cmc.getPriorNameMessage().isEmpty() &&
                !lastKnown.orElseGet(pl::name).equalsIgnoreCase(pl.name())) {
                    // Name change!
                    joinEvent.audience().orElse(joinEvent.originalAudience())
                            .sendMessage(this.priorNameMessage.getForObjectWithTokens(pl,
                                    Collections.singletonMap("previousname", cs -> Optional.of(Component.text(lastKnown.get())))));
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

        if (this.cmc.isModifyLoginMessage()) {
            if (this.cmc.getLoginMessage().isEmpty()) {
                joinEvent.setMessageCancelled(true);
                joinEvent.setAudience(null);
            } else {
                joinEvent.setMessage(this.loginMessage.getForObject(pl));
            }
        }
    }

    @Listener
    public void onPlayerFirstJoin(final NucleusFirstJoinEvent event, @Getter("getPlayer") final ServerPlayer pl) {
        if (this.cmc.isShowFirstTimeMessage() && !this.cmc.getFirstTimeMessage().isEmpty()) {
            event.audience().orElse(Sponge.server())
                    .sendMessage(this.firstTimeMessage.getForObject(pl));
        }
    }

    @Listener
    public void onPlayerQuit(final ServerSideConnectionEvent.Disconnect leaveEvent, @Getter("player") final ServerPlayer pl) {
        if (leaveEvent.message() == Component.empty()) {
            leaveEvent.setAudience(null);
            return;
        }

        if (this.cmc.isDisableWithPermission() &&
                this.permissionService.hasPermission(pl, ConnectionMessagesPermissions.CONNECTIONMESSSAGES_DISABLE)) {
            leaveEvent.setAudience(null);
            return;
        }

        if (this.cmc.isModifyLogoutMessage()) {
            if (this.cmc.getLogoutMessage().isEmpty()) {
                leaveEvent.setAudience(null);
            } else {
                leaveEvent.setMessage(this.logoutMessage.getForObject(pl));
            }
        }
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        this.cmc = serviceCollection.configProvider().getModuleConfig(ConnectionMessagesConfig.class);
        final INucleusTextTemplateFactory factory = serviceCollection.textTemplateFactory();
        this.firstTimeMessage = factory.createFromAmpersandString(this.cmc.getFirstTimeMessage());
        this.loginMessage = factory.createFromAmpersandString(this.cmc.getLoginMessage());
        this.logoutMessage = factory.createFromAmpersandString(this.cmc.getLogoutMessage());
        this.priorNameMessage = factory.createFromAmpersandString(this.cmc.getPriorNameMessage());
    }
}
