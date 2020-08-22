/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.serverlist.listener;

import io.github.nucleuspowered.nucleus.api.text.NucleusTextTemplate;
import io.github.nucleuspowered.nucleus.modules.serverlist.config.ServerListConfig;
import io.github.nucleuspowered.nucleus.modules.serverlist.services.ServerListService;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.texttemplatefactory.NucleusTextTemplateImpl;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.server.ClientPingServerEvent;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.Text;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class ServerListListener implements IReloadableService.Reloadable, ListenerBase.Conditional {

    private final ServerListService service;
    private final Random random = new Random();
    private ServerListConfig config = new ServerListConfig();

    @Inject
    public ServerListListener(INucleusServiceCollection serviceCollection) {
        this.service = serviceCollection.getServiceUnchecked(ServerListService.class);
    }

    @Listener
    public void onServerListPing(ClientPingServerEvent event, @Getter("getResponse") ClientPingServerEvent.Response response) {
        if (this.config.isModifyServerList()) {
            List<NucleusTextTemplateImpl> list = null;
            Optional<Text> ott = this.service.getMessage();

            if (ott.isPresent()) {
                response.setDescription(ott.get());
            } else {
                if (Sponge.getServer().hasWhitelist() && !this.config.getWhitelist().isEmpty()) {
                    list = this.config.getWhitelist();
                } else if (!this.config.getMessages().isEmpty()) {
                    list = this.config.getMessages();
                }

                if (list != null) {
                    NucleusTextTemplate template = list.get(this.random.nextInt(list.size()));
                    response.setDescription(template.getForCommandSource(Sponge.getServer().getConsole()));
                }
            }
        }

        if (this.config.isHidePlayerCount()) {
            response.setHidePlayers(true);
        } else if (this.config.isHideVanishedPlayers()) {
            Collection<GameProfile> players = Sponge.getServer().getOnlinePlayers().stream()
                    .filter(x -> !x.get(Keys.VANISH).orElse(false))
                    .map(User::getProfile).collect(Collectors.toList());

            response.getPlayers().ifPresent(y -> {
                y.getProfiles().clear();
                y.getProfiles().addAll(players);
                y.setOnline(players.size());
            });
        }
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        this.config = serviceCollection.moduleDataProvider().getModuleConfig(ServerListConfig.class);
    }

    @Override
    public boolean shouldEnable(INucleusServiceCollection serviceCollection) {
        return serviceCollection.moduleDataProvider().getModuleConfig(ServerListConfig.class).enableListener();
    }
}
