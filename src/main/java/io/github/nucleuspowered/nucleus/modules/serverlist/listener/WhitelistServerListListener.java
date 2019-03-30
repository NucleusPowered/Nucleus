/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.serverlist.listener;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.text.NucleusTextTemplate;
import io.github.nucleuspowered.nucleus.internal.interfaces.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.text.NucleusTextTemplateImpl;
import io.github.nucleuspowered.nucleus.modules.serverlist.ServerListModule;
import io.github.nucleuspowered.nucleus.modules.serverlist.config.ServerListConfig;
import io.github.nucleuspowered.nucleus.modules.serverlist.config.ServerListConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.serverlist.services.ServerListService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.server.ClientPingServerEvent;
import org.spongepowered.api.text.Text;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;

public class WhitelistServerListListener implements Reloadable, ListenerBase.Conditional {

    private final ServerListService service;
    private final Random random = new Random();
    private ServerListConfig config;

    public WhitelistServerListListener() {
        this.service = getServiceUnchecked(ServerListService.class);
    }

    @Listener(order = Order.LATE)
    public void onServerListPing(ClientPingServerEvent event, @Getter("getResponse") ClientPingServerEvent.Response response) {
        if (!Sponge.getServer().hasWhitelist()) {
            return;
        }

        Optional<Text> ott = this.service.getMessage();
        if (!ott.isPresent() &&  !this.config.getWhitelist().isEmpty()) {
            List<NucleusTextTemplateImpl> list = this.config.getWhitelist();

            if (list != null) {
                NucleusTextTemplate template = list.get(this.random.nextInt(list.size()));
                response.setDescription(template.getForCommandSource(Sponge.getServer().getConsole()));
            }
        }
    }

    @Override
    public void onReload() {
        this.config = Nucleus.getNucleus().getConfigValue(ServerListModule.ID, ServerListConfigAdapter.class, Function.identity())
                .orElseGet(ServerListConfig::new);
    }

    @Override
    public boolean shouldEnable() {
        return Nucleus.getNucleus().getConfigValue(ServerListModule.ID, ServerListConfigAdapter.class, ServerListConfig::enableWhitelistListener).orElse(false);
    }
}
