/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.info.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.modules.info.InfoModule;
import io.github.nucleuspowered.nucleus.modules.info.InfoPermissions;
import io.github.nucleuspowered.nucleus.modules.info.config.InfoConfig;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ITextFileControllerCollection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.plugin.PluginContainer;

import java.util.concurrent.TimeUnit;

public class InfoListener implements IReloadableService.Reloadable, ListenerBase.Conditional {

    private final IPermissionService permissionService;
    private final ITextFileControllerCollection textFileControllerCollection;
    private final PluginContainer pluginContainer;

    @Inject
    public InfoListener(final INucleusServiceCollection serviceCollection) {
        this.permissionService = serviceCollection.permissionService();
        this.textFileControllerCollection = serviceCollection.textFileControllerCollection();
        this.pluginContainer = serviceCollection.pluginContainer();
    }

    private boolean usePagination = true;
    private TextComponent title = Component.empty();

    private int delay = 500;

    @Listener
    public void playerJoin(final ServerSideConnectionEvent.Join event, @Getter("player") final ServerPlayer player) {
        // Send message one second later on the Async thread.
        Sponge.asyncScheduler().executor(this.pluginContainer).schedule(() -> {
                if (this.permissionService.hasPermission(player, InfoPermissions.MOTD_JOIN)) {
                    this.textFileControllerCollection.get(InfoModule.MOTD_KEY).ifPresent(x -> {
                        if (this.usePagination) {
                            x.sendToAudience(player, this.title);
                        } else {
                            x.getTextFromNucleusTextTemplates(player).forEach(player::sendMessage);
                        }
                    });
                }
            }, this.delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        final InfoConfig config = serviceCollection.configProvider().getModuleConfig(InfoConfig.class);
        this.delay = (int)(config.getMotdDelay() * 1000);
        this.usePagination = config.isMotdUsePagination();

        final String title = config.getMotdTitle();
        if (title.isEmpty()) {
            this.title = Component.empty();
        } else {
            this.title = LegacyComponentSerializer.legacyAmpersand().deserialize(title);
        }

    }

    @Override
    public boolean shouldEnable(final INucleusServiceCollection serviceCollection) {
        return serviceCollection.configProvider().getModuleConfig(InfoConfig.class).isShowMotdOnJoin();
    }
}
