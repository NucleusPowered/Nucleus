/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.info.listeners;

import io.github.nucleuspowered.nucleus.modules.info.InfoModule;
import io.github.nucleuspowered.nucleus.modules.info.InfoPermissions;
import io.github.nucleuspowered.nucleus.modules.info.config.InfoConfig;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.services.interfaces.ITextFileControllerCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class InfoListener implements IReloadableService.Reloadable, ListenerBase.Conditional {

    private final IPermissionService permissionService;
    private final ITextFileControllerCollection textFileControllerCollection;
    private final PluginContainer pluginContainer;

    @Inject
    public InfoListener(INucleusServiceCollection serviceCollection) {
        this.permissionService = serviceCollection.permissionService();
        this.textFileControllerCollection = serviceCollection.textFileControllerCollection();
        this.pluginContainer = serviceCollection.pluginContainer();
    }

    private boolean usePagination = true;
    private Text title = Text.EMPTY;

    private int delay = 500;

    @Listener
    public void playerJoin(ClientConnectionEvent.Join event, @Getter("getTargetEntity") Player player) {
        // Send message one second later on the Async thread.
        Sponge.getScheduler().createAsyncExecutor(this.pluginContainer).schedule(() -> {
                if (this.permissionService.hasPermission(player, InfoPermissions.MOTD_JOIN)) {
                    this.textFileControllerCollection.get(InfoModule.MOTD_KEY).ifPresent(x -> {
                        if (this.usePagination) {
                            x.sendToPlayer(player, this.title);
                        } else {
                            x.getTextFromNucleusTextTemplates(player).forEach(player::sendMessage);
                        }
                    });
                }
            }, this.delay, TimeUnit.MILLISECONDS);
    }

    @Override public void onReload(INucleusServiceCollection serviceCollection) {
        InfoConfig config = serviceCollection.moduleDataProvider().getModuleConfig(InfoConfig.class);
        this.delay = (int)(config.getMotdDelay() * 1000);
        this.usePagination = config.isMotdUsePagination();

        String title = config.getMotdTitle();
        if (title.isEmpty()) {
            this.title = Text.EMPTY;
        } else {
            this.title = TextSerializers.FORMATTING_CODE.deserialize(title);
        }

    }

    @Override public boolean shouldEnable(INucleusServiceCollection serviceCollection) {
        return serviceCollection.moduleDataProvider().getModuleConfig(InfoConfig.class).isShowMotdOnJoin();
    }
}
