/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mute.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.module.mute.data.Mute;
import io.github.nucleuspowered.nucleus.api.util.data.TimedEntry;
import io.github.nucleuspowered.nucleus.modules.mute.config.MuteConfig;
import io.github.nucleuspowered.nucleus.modules.mute.services.MuteService;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.manager.CommandMapping;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.ExecuteCommandEvent;
import org.spongepowered.api.event.filter.cause.Root;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MuteCommandListener implements ListenerBase.Conditional {

    private final List<String> blockedCommands = new ArrayList<>();

    private final INucleusServiceCollection serviceCollection;
    private final MuteService handler;

    @Inject
    public MuteCommandListener(final INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
        this.handler = serviceCollection.getServiceUnchecked(MuteService.class);
    }

    @Listener(order = Order.FIRST)
    public void onPlayerSendCommand(final ExecuteCommandEvent.Pre event, @Root final ServerPlayer player) {
        if (!this.handler.isMuted(player.uniqueId())) {
            return;
        }

        final String command = event.command().toLowerCase();
        final Optional<? extends CommandMapping> oc = Sponge.server().commandManager().commandMapping(command);
        final Set<String> cmd;

        // If the command exists, then get all aliases.
        cmd = oc.map(commandMapping -> commandMapping.allAliases().stream().map(String::toLowerCase).collect(Collectors.toSet()))
                .orElseGet(() -> Collections.singleton(command));

        // If the command is in the list, block it.
        if (this.blockedCommands.stream().map(String::toLowerCase).anyMatch(cmd::contains)) {
            final Mute muteData = this.handler.getPlayerMuteInfo(player.uniqueId()).orElse(null);
            if (muteData == null || muteData.getTimedEntry().map(TimedEntry::expired).orElse(false)) {
                this.handler.unmutePlayer(player.uniqueId());
            } else {
                this.handler.onMute(muteData, player);
                Sponge.systemSubject().sendMessage(LinearComponents.linear(
                        Component.text(player.name() + "("),
                        this.serviceCollection.messageProvider().getMessage("standard.muted"),
                        Component.text("): "),
                        Component.text("/" + event.command() + " " + event.arguments())
                ));
                event.setCancelled(true);
            }
        }
    }

    // will also act as the reloadable.
    @Override
    public boolean shouldEnable(final INucleusServiceCollection serviceCollection) {
        this.blockedCommands.clear();
        this.blockedCommands.addAll(serviceCollection.configProvider().getModuleConfig(MuteConfig.class).getBlockedCommands());
        return !this.blockedCommands.isEmpty();
    }
}
