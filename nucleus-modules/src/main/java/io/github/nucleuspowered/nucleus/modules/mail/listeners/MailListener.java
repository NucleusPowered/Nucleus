/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mail.listeners;

import io.github.nucleuspowered.nucleus.modules.mail.services.MailHandler;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class MailListener implements ListenerBase {

    private final PluginContainer pluginContainer;
    private final MailHandler handler;
    private final IMessageProviderService messageProvider;

    @Inject
    public MailListener(INucleusServiceCollection serviceCollection) {
        this.messageProvider = serviceCollection.messageProvider();
        this.handler = serviceCollection.getServiceUnchecked(MailHandler.class);
        this.pluginContainer = serviceCollection.pluginContainer();
    }

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event, @Getter("getTargetEntity") Player player) {
        Sponge.getScheduler().createAsyncExecutor(this.pluginContainer).schedule(() -> {
            int mailCount = this.handler.getMailInternal(event.getTargetEntity()).size();
            if (mailCount > 0) {
                this.messageProvider.sendMessageTo(player, "mail.login", String.valueOf(mailCount));
                player.sendMessage(Text.builder()
                        .append(Text.builder("/mail").color(TextColors.AQUA).style(TextStyles.UNDERLINE).onClick(TextActions.runCommand("/mail"))
                                .onHover(TextActions.showText(Text.of("Click here to read your mail."))).build())
                        .append(Text.builder().append(Text.of(TextColors.YELLOW, " ")).append(
                                this.messageProvider.getMessageFor(player, "mail.toread"))
                                .append(Text.of(" ")).build())
                        .append(Text.builder("/mail clear").color(TextColors.AQUA).style(TextStyles.UNDERLINE)
                                .onClick(TextActions.runCommand("/mail clear"))
                                .onHover(TextActions.showText(Text.of("Click here to delete your mail."))).build())
                        .append(Text.builder().append(Text.of(TextColors.YELLOW, " ")).append(
                                this.messageProvider.getMessageFor(player, "mail.toclear")).build())
                        .build());
            }
        } , 1, TimeUnit.SECONDS);
    }
}
