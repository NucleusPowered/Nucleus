/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mail.listeners;

import io.github.nucleuspowered.nucleus.modules.mail.services.MailHandler;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.plugin.PluginContainer;

public class MailListener implements ListenerBase {

    private final PluginContainer pluginContainer;
    private final MailHandler handler;
    private final IMessageProviderService messageProvider;

    @Inject
    public MailListener(final INucleusServiceCollection serviceCollection) {
        this.messageProvider = serviceCollection.messageProvider();
        this.handler = serviceCollection.getServiceUnchecked(MailHandler.class);
        this.pluginContainer = serviceCollection.pluginContainer();
    }

    @Listener
    public void onPlayerJoin(final ServerSideConnectionEvent.Join event, @Getter("player") final ServerPlayer player) {
        final UUID uuid = player.uniqueId();
        Sponge.asyncScheduler().executor(this.pluginContainer).schedule(() -> {
            final int mailCount = this.handler.getMailInternal(uuid).size();
            if (mailCount > 0) {
                this.messageProvider.sendMessageTo(player, "mail.login", String.valueOf(mailCount));
                player.sendMessage(
                        Identity.nil(),
                        LinearComponents.linear(
                                Component.text().content("/nucleus:mail")
                                        .color(NamedTextColor.AQUA)
                                        .style(Style.style(TextDecoration.UNDERLINED))
                                        .clickEvent(ClickEvent.runCommand("/nucleus:mail"))
                                        .hoverEvent(HoverEvent.showText(this.messageProvider.getMessage("mail.readhint")))
                                        .build(),
                                Component.space(),
                                this.messageProvider.getMessageFor(player, "mail.toread"),
                                Component.space(),
                                Component.text()
                                        .content("/nucleus:mail clear")
                                        .color(NamedTextColor.AQUA)
                                        .style(Style.style(TextDecoration.UNDERLINED))
                                        .clickEvent(ClickEvent.runCommand("/nucleus:mail clear"))
                                        .hoverEvent(HoverEvent.showText(this.messageProvider.getMessage("mail.deletehint")))
                                        .build(),
                                Component.space(),
                                this.messageProvider.getMessageFor(player, "mail.toclear")));

            }
        } , 1, TimeUnit.SECONDS);
    }
}
