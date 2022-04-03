/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.chatlogger.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.module.mail.event.NucleusSendMailEvent;
import io.github.nucleuspowered.nucleus.modules.chatlogger.config.ChatLoggingConfig;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.profile.GameProfile;

public class MailLoggingListener extends AbstractLoggerListener {

    @Inject
    public MailLoggingListener(final INucleusServiceCollection serviceCollection) {
        super(serviceCollection);
    }

    @Listener(order = Order.LAST)
    public void onCommand(final NucleusSendMailEvent event, @First final ServerPlayer source) {
        final String message = this.messageProviderService.getMessageString("chatlog.mail",
            source.name(), Sponge.server().gameProfileManager().cache().findById(event.getRecipient()).flatMap(GameProfile::name).orElse("null"),
                event.getMessage());
        this.handler.queueEntry(message);
    }

    @Override public boolean shouldEnable(final INucleusServiceCollection serviceCollection) {
        final ChatLoggingConfig config = this.getConfig(serviceCollection);
        return config.isEnableLog() && config.isLogMail();
    }
}
