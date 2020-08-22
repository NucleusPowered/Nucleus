/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.vanish.listener;

import io.github.nucleuspowered.nucleus.modules.vanish.VanishKeys;
import io.github.nucleuspowered.nucleus.modules.vanish.VanishPermissions;
import io.github.nucleuspowered.nucleus.modules.vanish.config.VanishConfig;
import io.github.nucleuspowered.nucleus.modules.vanish.services.VanishService;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.userprefs.NucleusKeysProvider;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.services.interfaces.IStorageManager;
import io.github.nucleuspowered.nucleus.services.interfaces.IUserPreferenceService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.action.TextActions;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

public class VanishListener implements IReloadableService.Reloadable, ListenerBase {

    private VanishConfig vanishConfig = new VanishConfig();
    private final PluginContainer pluginContainer;
    private final VanishService service;
    private final IPermissionService permissionService;
    private final IMessageProviderService messageProviderService;
    private final IUserPreferenceService userPreferenceService;
    private final IStorageManager storageManager;

    @Inject
    public VanishListener(INucleusServiceCollection serviceCollection) {
        this.pluginContainer = serviceCollection.pluginContainer();
        this.service = serviceCollection.getServiceUnchecked(VanishService.class);
        this.permissionService = serviceCollection.permissionService();
        this.messageProviderService = serviceCollection.messageProvider();
        this.userPreferenceService = serviceCollection.userPreferenceService();
        this.storageManager = serviceCollection.storageManager();
    }

    @Listener
    public void onAuth(ClientConnectionEvent.Auth auth) {
        if (this.vanishConfig.isTryHidePlayers()) {
            UUID uuid = auth.getProfile().getUniqueId();
            CompletableFuture<Void> future = new CompletableFuture<>();
            Task.builder().execute(
                    () -> {
                        Sponge.getServiceManager()
                                .provideUnchecked(UserStorageService.class)
                                .get(uuid)
                                .flatMap(x -> x.get(Keys.LAST_DATE_PLAYED))
                                .ifPresent(y -> this.service.setLastVanishedTime(uuid, y));
                        future.complete(null);
                    }
            ).submit(this.pluginContainer);

            future.join();
        }
    }

    @Listener
    public void onLogin(ClientConnectionEvent.Join event, @Getter("getTargetEntity") Player player) {
        boolean persist = this.service.isVanished(player);

        boolean shouldVanish = (this.permissionService.hasPermission(player, VanishPermissions.VANISH_ONLOGIN)
                && this.userPreferenceService.get(player.getUniqueId(), NucleusKeysProvider.VANISH_ON_LOGIN).orElse(false))
                || persist;

        if (shouldVanish) {
            if (!this.permissionService.hasPermission(player, VanishPermissions.VANISH_PERSIST)) {
                // No permission, no vanish.
                this.service.unvanishPlayer(player);
                return;
            } else if (this.vanishConfig.isSuppressMessagesOnVanish()) {
                event.setMessageCancelled(true);
            }

            this.service.vanishPlayer(player, true);
            this.messageProviderService.sendMessageTo(player, "vanish.login");

            if (!persist) {
                // on login
                player.sendMessage(this.messageProviderService.getMessageFor(player, "vanish.onlogin.prefs").toBuilder()
                        .onClick(TextActions.runCommand("/nuserprefs vanish-on-login false")).build());
            }
        } else if (this.vanishConfig.isForceNucleusVanish()) {
            // unvanish
            this.service.unvanishPlayer(player);
        }
    }

    @Listener
    public void onQuit(ClientConnectionEvent.Disconnect event, @Getter("getTargetEntity") Player player) {
        if (player.get(Keys.VANISH).orElse(false)) {
            this.storageManager.getUserService().get(player.getUniqueId())
                    .thenAccept(x -> x.ifPresent(t -> t.set(VanishKeys.VANISH_STATUS, false)));
            if (this.vanishConfig.isSuppressMessagesOnVanish()) {
                event.setMessageCancelled(true);
            }
        }

        this.service.clearLastVanishTime(player.getUniqueId());
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        this.vanishConfig = serviceCollection.moduleDataProvider().getModuleConfig(VanishConfig.class);
    }
}
