/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.vanish.listener;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.modules.vanish.VanishKeys;
import io.github.nucleuspowered.nucleus.modules.vanish.VanishPermissions;
import io.github.nucleuspowered.nucleus.modules.vanish.config.VanishConfig;
import io.github.nucleuspowered.nucleus.modules.vanish.services.VanishService;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IStorageManager;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IUserPreferenceService;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.event.ClickEvent;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;

import java.util.UUID;

public class VanishListener implements IReloadableService.Reloadable, ListenerBase {

    private VanishConfig vanishConfig = new VanishConfig();
    private final VanishService service;
    private final IPermissionService permissionService;
    private final IMessageProviderService messageProviderService;
    private final IUserPreferenceService userPreferenceService;
    private final IStorageManager storageManager;

    @Inject
    public VanishListener(final INucleusServiceCollection serviceCollection) {
        this.service = serviceCollection.getServiceUnchecked(VanishService.class);
        this.permissionService = serviceCollection.permissionService();
        this.messageProviderService = serviceCollection.messageProvider();
        this.userPreferenceService = serviceCollection.userPreferenceService();
        this.storageManager = serviceCollection.storageManager();
    }

    @Listener(order = Order.LAST)
    public void onAuth(final ServerSideConnectionEvent.Auth auth) {
        if (this.vanishConfig.isTryHidePlayers()) {
            final UUID uuid = auth.profile().uuid();
            Sponge.server().userManager()
                                .load(uuid)
                                .join()
                                .flatMap(x -> x.get(Keys.LAST_DATE_PLAYED))
                                .ifPresent(y -> this.service.setLastVanishedTime(uuid, y));
        }
    }

    @Listener
    public void onLogin(final ServerSideConnectionEvent.Join event, @Getter("player") final ServerPlayer player) {
        final boolean persist = this.service.isVanished(player.uniqueId());

        final boolean shouldVanish = (this.permissionService.hasPermission(player, VanishPermissions.VANISH_ONLOGIN)
                && this.userPreferenceService.get(player.uniqueId(), VanishKeys.VANISH_ON_LOGIN).orElse(false))
                || persist;

        if (shouldVanish) {
            if (!this.permissionService.hasPermission(player, VanishPermissions.VANISH_PERSIST)) {
                // No permission, no vanish.
                this.service.unvanishPlayer(player.user());
                return;
            } else if (this.vanishConfig.isSuppressMessagesOnVanish()) {
                event.setMessageCancelled(true);
            }

            this.service.vanishPlayer(player.user(), true);
            this.messageProviderService.sendMessageTo(player, "vanish.login");

            if (!persist) {
                // on login
                player.sendMessage(this.messageProviderService.getMessageFor(player, "vanish.onlogin.prefs")
                    .clickEvent(ClickEvent.runCommand("/nuserprefs vanish-on-login false")));
            }
        } else if (this.vanishConfig.isForceNucleusVanish()) {
            // unvanish
            this.service.unvanishPlayer(player.user());
        }
    }

    @Listener
    public void onQuit(final ServerSideConnectionEvent.Disconnect event, @Getter("player") final ServerPlayer player) {
        if (player.get(Keys.VANISH).orElse(false)) {
            this.storageManager.getUserService().get(player.uniqueId())
                    .thenAccept(x -> x.ifPresent(t -> t.set(VanishKeys.VANISH_STATUS, false)));
            if (this.vanishConfig.isSuppressMessagesOnVanish()) {
                event.setAudience(Audience.empty());
            }
        }

        this.service.clearLastVanishTime(player.uniqueId());
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        this.vanishConfig = serviceCollection.configProvider().getModuleConfig(VanishConfig.class);
    }
}
